package com.dreamdisplays.media.source.bilibili

import com.dreamdisplays.api.media.common.DreamMediaException
import com.dreamdisplays.api.media.source.MediaResolver
import com.dreamdisplays.api.media.source.MediaSource
import com.dreamdisplays.api.media.source.ResolvedMedia
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * In-process Bilibili resolver: one site-API call (see [BilibiliApi]) instead of a `yt-dlp` subprocess, mirroring the
 * other first-party resolvers.
 */
object BilibiliResolver : MediaResolver {
    /** Logger. */
    private val logger = LoggerFactory.getLogger("DreamDisplays/BilibiliResolver")

    /** Alongside the other first-party in-process resolvers, above the `yt-dlp` fallback. */
    override val priority: Int = 10

    /** Live playurl URLs are session-bound, so live entries expire fast; VODs are stable. */
    private class Entry(val value: ResolvedMedia, val ttlNanos: Long)

    private val cache: Cache<String, Entry> = Caffeine.newBuilder()
        .maximumSize(64)
        .expireAfter(object : Expiry<String, Entry> {
            override fun expireAfterCreate(key: String, value: Entry, currentTime: Long) = value.ttlNanos
            override fun expireAfterUpdate(key: String, value: Entry, currentTime: Long, currentDuration: Long) =
                value.ttlNanos

            override fun expireAfterRead(key: String, value: Entry, currentTime: Long, currentDuration: Long) =
                currentDuration
        })
        .build()

    private val LIVE_TTL_NANOS = TimeUnit.SECONDS.toNanos(25)
    private val VOD_TTL_NANOS = TimeUnit.MINUTES.toNanos(30)

    override fun canResolve(source: MediaSource): Boolean = source is MediaSource.Bilibili

    override fun prefetch(source: MediaSource): Boolean {
        val bilibili = source as? MediaSource.Bilibili ?: return false
        return runCatching { resolveCached(bilibili) }.isSuccess
    }

    override fun resolve(source: MediaSource): ResolvedMedia {
        val bilibili = source as? MediaSource.Bilibili
            ?: throw UnsupportedOperationException("$source is not a Bilibili source.")
        return resolveCached(bilibili)
    }

    /** Drops [url]'s cached resolution so a dying live playurl is re-minted, not re-served. */
    fun invalidate(url: String) {
        (MediaSource.from(url) as? MediaSource.Bilibili)?.let { source ->
            BilibiliMetadataCache.cacheKey(source)?.let(cache::invalidate)
        }
    }

    private fun resolveCached(source: MediaSource.Bilibili): ResolvedMedia {
        val key = BilibiliMetadataCache.cacheKey(source)
            ?: throw DreamMediaException.NotFound("Unrecognized Bilibili URL: ${source.url}.")
        cache.getIfPresent(key)?.let { return it.value }

        val playback = BilibiliApi.resolve(source)
            ?: throw DreamMediaException.NotFound("Bilibili video/room could not be reached.")
        BilibiliMetadataCache.put(source, playback.metadata)

        if (playback.streams.isEmpty()) {
            throw DreamMediaException.NotFound(
                if (source.roomId != null) "This Bilibili live room is offline right now."
                else "This Bilibili video is unavailable or private.",
            )
        }

        logger.debug(
            "Resolved Bilibili {}: {} streams, live={}.", key, playback.streams.size, playback.metadata.isLive,
        )
        val resolved = ResolvedMedia(
            streams = playback.streams,
            metadata = playback.metadata.toMediaMetadata(),
            isLive = playback.metadata.isLive,
            isSeekable = playback.isSeekable,
        )
        cache.put(key, Entry(resolved, if (playback.metadata.isLive) LIVE_TTL_NANOS else VOD_TTL_NANOS))
        return resolved
    }
}
