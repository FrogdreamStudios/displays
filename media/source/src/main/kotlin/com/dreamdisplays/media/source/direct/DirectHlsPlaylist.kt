package com.dreamdisplays.media.source.direct

import java.net.URI

/**
 * Minimal HLS playlist reader for user-pasted `.m3u8` URLs.
 *
 * Only what the resolver actually needs: split master from media playlist, read the variant ladder
 * so quality selection works on a custom HLS link the same way it does on a platform stream, and
 * tell a live playlist from a finished one. Segment parsing stays with the decoder, which reads the
 * playlist itself anyway.
 *
 * Unlike the Twitch playlists (always absolute URLs from usher), a third-party master playlist
 * usually lists its variants as relative paths, so every URI is resolved against the playlist URL.
 */
internal object DirectHlsPlaylist {
    /** One `#EXT-X-STREAM-INF` entry of a master playlist. */
    data class Variant(
        val url: String,
        val width: Int?,
        val height: Int?,
        val fps: Double?,
        val bandwidthBps: Int?,
        val codecs: String?,
        /** The `AUDIO` group this variant takes its sound from, or null when the audio is muxed in. */
        val audioGroupId: String?,
    )

    /**
     * One `#EXT-X-MEDIA:TYPE=AUDIO` rendition that lives in its own playlist.
     *
     * A master playlist that declares these has *video-only* variants: the sound is a separate
     * playlist the player is expected to fetch alongside. Renditions without a `URI` describe audio
     * already muxed into the variants and are not represented here.
     */
    data class AudioRendition(
        val url: String,
        val groupId: String,
        val name: String?,
        val language: String?,
        val isDefault: Boolean,
    )

    /**
     * A parsed playlist.
     *
     * @property variants the master playlist's renditions; empty for a media playlist.
     * @property audioRenditions separate audio playlists referenced by [Variant.audioGroupId].
     * @property isLive true when the playlist can still grow — a media playlist with no
     * `#EXT-X-ENDLIST`, which is exactly the shape a live stream has.
     */
    data class Parsed(
        val variants: List<Variant>,
        val audioRenditions: List<AudioRendition>,
        val isLive: Boolean,
        /**
         * True when the playlist declares an `#EXT-X-MAP` initialization segment, i.e. it is
         * fragmented MP4 rather than MPEG-TS. `FFmpeg`'s HLS demuxer loses that init segment when it
         * seeks past the opening segments and then decodes nothing at all, so this decides how the
         * player is allowed to seek (see `MediaStream.seekByDecoding`).
         */
        val hasInitSegment: Boolean = false,

        /**
         * Sum of the playlist's `#EXTINF` durations, or 0 for a master (which lists no segments) and
         * for a live playlist (where the total is only the window currently published, not the
         * media). This is the one place an HLS VOD's length is known without opening the stream, and
         * the player needs it: without a duration there is no timeline, and so no seek bar at all.
         */
        val totalDurationNanos: Long = 0L,
    ) {
        /** True when this is a master playlist, i.e. it lists renditions rather than segments. */
        val isMaster: Boolean get() = variants.isNotEmpty()

        /** Separate audio playlists usable by [variant], best (default) first. */
        fun audioFor(variant: Variant): List<AudioRendition> {
            val group = variant.audioGroupId ?: return emptyList()
            return audioRenditions.filter { it.groupId == group }.sortedByDescending { it.isDefault }
        }
    }

    /** True when [text] looks like any HLS playlist at all. */
    fun looksLikePlaylist(text: String): Boolean = text.trimStart().startsWith("#EXTM3U")

    /**
     * Parses [text], resolving every variant URI against [baseUrl].
     *
     * A media playlist is treated as live unless it declares `#EXT-X-ENDLIST` or
     * `#EXT-X-PLAYLIST-TYPE:VOD`. A master playlist says nothing about liveness on its own, so it
     * is reported as non-live and the decoder discovers the truth from the media playlist it picks.
     */
    fun parse(text: String, baseUrl: String): Parsed {
        val variants = ArrayList<Variant>()
        val audio = ArrayList<AudioRendition>()
        val seenVariantUrls = HashSet<String>()
        var pending: Map<String, String>? = null
        var hasSegments = false
        var ended = false
        var vod = false
        var hasInit = false
        var segmentNanos = 0L

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("#EXT-X-STREAM-INF:") ->
                    pending = parseAttributes(line.removePrefix("#EXT-X-STREAM-INF:"))

                line.startsWith("#EXT-X-MEDIA:") ->
                    parseAudioRendition(parseAttributes(line.removePrefix("#EXT-X-MEDIA:")), baseUrl)
                        ?.let(audio::add)

                line.startsWith("#EXT-X-MAP") -> hasInit = true
                line.startsWith("#EXTINF") -> {
                    hasSegments = true
                    segmentNanos += extInfNanos(line)
                }
                line.startsWith("#EXT-X-ENDLIST") -> ended = true
                line.startsWith("#EXT-X-PLAYLIST-TYPE:") ->
                    vod = line.substringAfter(':').trim().equals("VOD", ignoreCase = true)

                line.startsWith("#") -> {}

                else -> {
                    val attrs = pending ?: continue
                    pending = null
                    val url = resolve(baseUrl, line)
                    // The same video rendition is listed once per audio group it can pair with
                    // (Apple's reference master lists every variant three times, for stereo / AC-3 /
                    // Dolby). They are one entry in the quality ladder, not three.
                    if (!seenVariantUrls.add(url)) continue
                    val resolution = attrs["RESOLUTION"]?.split('x', limit = 2)
                    variants.add(
                        Variant(
                            url = url,
                            width = resolution?.getOrNull(0)?.toIntOrNull(),
                            height = resolution?.getOrNull(1)?.toIntOrNull(),
                            fps = attrs["FRAME-RATE"]?.toDoubleOrNull(),
                            bandwidthBps = attrs["BANDWIDTH"]?.toIntOrNull(),
                            codecs = attrs["CODECS"]?.substringBefore(','),
                            audioGroupId = attrs["AUDIO"]?.takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
        }

        return Parsed(
            variants = variants.sortedByDescending { it.height ?: it.bandwidthBps ?: 0 },
            audioRenditions = audio,
            isLive = variants.isEmpty() && hasSegments && !ended && !vod,
            hasInitSegment = hasInit,
            totalDurationNanos = segmentNanos,
        )
    }

    /** Content duration of an `#EXTINF:<seconds>,<title>` line; 0 when it cannot be read. */
    private fun extInfNanos(line: String): Long {
        val seconds = line.substringAfter(':', "").substringBefore(',').trim().toDoubleOrNull() ?: return 0L
        if (!seconds.isFinite() || seconds <= 0.0) return 0L
        return (seconds * 1_000_000_000.0).toLong()
    }

    /**
     * Reads one `#EXT-X-MEDIA` tag, keeping only audio renditions that live in their own playlist.
     * A `TYPE=AUDIO` tag without a `URI` means the audio is already inside the variants, and a
     * subtitle / closed-caption tag is not something a display can play.
     */
    private fun parseAudioRendition(attrs: Map<String, String>, baseUrl: String): AudioRendition? {
        if (!attrs["TYPE"].equals("AUDIO", ignoreCase = true)) return null
        val uri = attrs["URI"]?.takeIf { it.isNotBlank() } ?: return null
        val group = attrs["GROUP-ID"]?.takeIf { it.isNotBlank() } ?: return null
        return AudioRendition(
            url = resolve(baseUrl, uri),
            groupId = group,
            name = attrs["NAME"]?.takeIf { it.isNotBlank() },
            language = attrs["LANGUAGE"]?.takeIf { it.isNotBlank() },
            isDefault = attrs["DEFAULT"].equals("YES", ignoreCase = true),
        )
    }

    /** Resolves a possibly relative playlist [reference] against the absolute [baseUrl]. */
    private fun resolve(baseUrl: String, reference: String): String =
        runCatching { URI(baseUrl).resolve(reference).toString() }.getOrDefault(reference)

    /**
     * Splits an HLS attribute list (`KEY=VALUE,KEY="quoted,value"`) into a map, honouring quotes so
     * a comma inside `CODECS="avc1.64001f,mp4a.40.2"` does not split the attribute.
     */
    private fun parseAttributes(text: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val current = StringBuilder()
        var quoted = false
        val parts = ArrayList<String>()
        for (c in text) {
            when (c) {
                '"' -> quoted = !quoted
                ',' if !quoted -> {
                    parts.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) parts.add(current.toString())
        for (part in parts) {
            val key = part.substringBefore('=').trim()
            if (key.isEmpty() || !part.contains('=')) continue
            out[key] = part.substringAfter('=').trim()
        }
        return out
    }
}
