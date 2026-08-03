package com.dreamdisplays.media.player.process

import com.dreamdisplays.media.player.pipeline.VideoFramePipe
import com.dreamdisplays.media.runtime.MediaHostGuard
import kotlinx.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Builds `FFmpeg` process invocations for the media pipeline and handles their
 * graceful shutdown.
 */
object MediaProcess {
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /** Kill switch for GPU-side scaling (`-Ddreamdisplays.hwscale=false` falls back to software scale). */
    private val HW_SCALE_ENABLED = System.getProperty("dreamdisplays.hwscale", "true").toBoolean()

    /** Frame rate assumed when the source reports none, or reports something implausible. */
    const val DEFAULT_OUTPUT_FPS = 30.0

    /**
     * Sanitizes a source frame rate into the constant rate the pipeline runs at.
     *
     * The raw-pipe transports carry no timestamps, so a frame's presentation time is synthesized as
     * `offset + frameIndex / fps`. That is only correct if `FFmpeg` really emits `fps` frames per
     * second of content — otherwise the synthesized timeline runs at a different rate than the audio
     * clock it is paced against, and lip sync drifts linearly: stream metadata rounding 30000/1001
     * to "30" is a 0.1 % error, which is 3.6 s of drift per hour. So the same value returned here is
     * both pinned into `FFmpeg`'s output rate ([videoArgs]) and used for the PTS arithmetic, which
     * makes the two consistent by construction even when the metadata is wrong.
     */
    fun outputFps(sourceFps: Double?): Double =
        sourceFps?.takeIf { it.isFinite() && it > 1.0 && it <= 240.0 } ?: DEFAULT_OUTPUT_FPS

    /**
     * True when [stderr] is `FFmpeg` reporting that the input simply has no audio track, rather than
     * any kind of failure to reach or decode it.
     *
     * `-vn` on a video-only input leaves the output with nothing to write, and `FFmpeg` refuses to
     * open it. This is the normal shape of a silent film, a screen recording, or an HLS variant whose
     * sound lives in a separate rendition — not a dying session, so the caller must play on in
     * silence instead of restarting. A transport failure is excluded explicitly: an input that never
     * opened produces the same empty output, and that one *is* worth retrying.
     */
    fun indicatesNoAudioStream(stderr: String): Boolean {
        if (!stderr.contains("does not contain any stream")) return false
        return NO_AUDIO_DISQUALIFIERS.none { stderr.contains(it, ignoreCase = true) }
    }

    /** Markers that mean the empty output came from a failed input, not from a silent one. */
    private val NO_AUDIO_DISQUALIFIERS = listOf(
        "Server returned", "Connection refused", "Connection timed out", "Invalid data found",
        "No such file or directory", "Protocol not found", "Immediate exit requested",
        "Error opening input", "Unknown error", "I/O error", "HTTP error",
    )

    /** Wire format the video `FFmpeg` process writes to its stdout pipe. */
    enum class VideoTransport {
        /** PPM frames (header + RGB24), parsed by the JVM [VideoFramePipe]. */
        PPM,

        /** Headerless RGB24 rawvideo, consumed by the native pipeline. */
        RAW_RGB24,

        /**
         * Headerless NV12 rawvideo, consumed by the native pipeline. Halves pipe traffic vs.
         * RGB24; the stream is pinned to BT.709 so the native converter can use a fixed matrix.
         */
        RAW_NV12,
    }

    /**
     * Builds an `FFmpeg` process to read video frames from [url] at the given [offsetNanos], scaled and cropped to [w] x [h].
     *
     * When [hwAccel] is anything other than [HwAccelBackend.NONE], the decoder is asked to run on
     * the GPU's video block. The pipeline still pulls RGB out via a pipe, so `FFmpeg` inserts an
     * implicit `hwdownload`; the win is that the heavy decoding step no longer burns a CPU core.
     *
     * @throws IOException if the process fails to start. the caller is responsible for destroying the process when done.
     */
    @Throws(IOException::class)
    fun buildVideo(
        ffmpeg: String, url: String, w: Int, h: Int, offsetNanos: Long, hwAccel: HwAccelBackend, fps: Double,
        alreadyResolved: Boolean = false, seekByDecoding: Boolean = false,
    ): Process =
        ProcessBuilder(
            videoArgs(
                ffmpeg, url, w, h, offsetNanos, hwAccel, VideoTransport.PPM, fps, alreadyResolved, seekByDecoding,
            ),
        ).start()

    /**
     * Builds the full `FFmpeg` argv for a video session emitting [transport] on stdout.
     * Used directly by the native pipeline (which spawns the process itself) and by [buildVideo].
     *
     * [fps] must be the value the reader uses for its synthesized frame timestamps (see [outputFps]);
     * it is pinned as the output frame rate so the emitted stream is constant-rate at exactly the
     * cadence the reader assumes.
     *
     * [alreadyResolved] skips the redirect walk for callers that ran [url] through
     * `MediaHostGuard.resolveSafeUrl` themselves — the in-process libav path needs the resolved URL
     * before it ever reaches here, so without this the same chain was walked twice per launch.
     */
    fun videoArgs(
        ffmpeg: String, url: String, w: Int, h: Int, offsetNanos: Long, hwAccel: HwAccelBackend,
        transport: VideoTransport, fps: Double, alreadyResolved: Boolean = false,
        seekByDecoding: Boolean = false,
    ): List<String> {
        val outFormat = if (transport == VideoTransport.RAW_NV12) "nv12" else "rgb24"
        val pad = "pad=w=$w:h=$h:x=(ow-iw)/2:y=(oh-ih)/2:color=black,setsar=1,format=$outFormat"
        val vf = if (useHwScale(ffmpeg, hwAccel)) {
            // Scale on the GPU's video block before hwdownload so the CPU never touches
            // full-resolution frames.
            val fit = "min($w/iw\\,$h/ih)"
            val matrix = if (transport == VideoTransport.RAW_NV12) ":color_matrix=bt709" else ""
            "scale_vt=w=trunc($fit*iw/2)*2:h=trunc($fit*ih/2)*2$matrix,hwdownload,format=nv12,$pad"
        } else {
            val swPrefix = if (hwAccel.hwOutputFormat != null) "hwdownload,format=nv12," else ""
            val scaleExtra = if (transport == VideoTransport.RAW_NV12) ":out_color_matrix=bt709" else ""
            val fitSw = "min($w/iw\\,$h/ih)"
            "${swPrefix}scale=w=min($w\\,iw*$fitSw):h=min($h\\,ih*$fitSw):flags=bilinear$scaleExtra,$pad"
        }
        return baseCommand(ffmpeg, url, offsetNanos, hwAccel, alreadyResolved, seekByDecoding).apply {
            addAll(listOf("-an", "-vf", vf))
            addAll(listOf("-r", String.format(Locale.US, "%.6f", outputFps(fps))))
            when (transport) {
                VideoTransport.PPM -> addAll(listOf("-f", "image2pipe", "-c:v", "ppm", "-"))
                VideoTransport.RAW_RGB24, VideoTransport.RAW_NV12 -> addAll(listOf("-f", "rawvideo", "-"))
            }
        }
    }

    /** True when the GPU-side scale chain should be used instead of software scaling. */
    private fun useHwScale(ffmpeg: String, hwAccel: HwAccelBackend): Boolean =
        HW_SCALE_ENABLED
                && hwAccel == HwAccelBackend.VIDEOTOOLBOX
                && FFmpegCapabilities.hasFilter(ffmpeg, "scale_vt")

    /**
     * Builds an `FFmpeg` process that seeks to [offsetNanos] in [url] and writes a single JPEG frame
     * (letterboxed to exactly [w] x [h], high-ish quality) to stdout. Used to generate seek-bar
     * scrub-preview thumbnails; the caller reads `process.inputStream` fully after `waitFor()`.
     *
     * [url] must already be redirect-resolved / host-checked (via `MediaHostGuard.resolveSafeUrl`) —
     * this is called once per sample across many short-lived processes for the same video, so
     * re-resolving here (an extra HTTP round-trip per call) would multiply that cost badly.
     *
     * @throws IOException if the process fails to start. The caller is responsible for destroying the process when done.
     */
    @Throws(IOException::class)
    fun buildFrameExtract(
        ffmpeg: String, url: String, offsetNanos: Long, w: Int, h: Int, seekByDecoding: Boolean = false,
    ): Process {
        val fitSw = "min($w/iw\\,$h/ih)"
        val pad = "scale=w=min($w\\,iw*$fitSw):h=min($h\\,ih*$fitSw):flags=lanczos," +
                "pad=w=$w:h=$h:x=(ow-iw)/2:y=(oh-ih)/2:color=black"
        val cmd = baseCommand(
            ffmpeg, url, offsetNanos, HwAccelBackend.NONE,
            alreadyResolved = true, seekByDecoding = seekByDecoding,
        ).apply {
            addAll(
                listOf(
                    "-an", "-frames:v", "1",
                    "-threads", "1",
                    "-vf", pad, "-q:v", "3",
                    "-f", "image2pipe", "-vcodec", "mjpeg", "-",
                )
            )
        }
        return ProcessBuilder(cmd).start()
    }

    /**
     * Builds an `FFmpeg` process to read audio samples from [url] at the given [offsetNanos], resampled to [sampleRate] Hz.
     * @throws IOException if the process fails to start. The caller is responsible for destroying the process when done.
     */
    @Throws(IOException::class)
    fun buildAudio(
        ffmpeg: String, url: String, offsetNanos: Long, sampleRate: Int, seekByDecoding: Boolean = false,
    ): Process {
        val cmd = baseCommand(
            ffmpeg, url, offsetNanos, HwAccelBackend.NONE, seekByDecoding = seekByDecoding,
        ).apply {
            addAll(listOf("-vn", "-f", "s16le", "-ar", sampleRate.toString(), "-ac", "2", "-"))
        }
        return ProcessBuilder(cmd).start()
    }

    /**
     * Builds an `FFmpeg` process that decodes audio from MPEG-TS piped into its stdin (see
     * `HlsAudioFeeder`), resampled to [sampleRate] Hz. No network flags: the process never opens a
     * connection itself, which sidesteps the bundled binary's flaky TLS on live HLS hosts.
     *
     * @throws IOException if the process fails to start. The caller is responsible for destroying it.
     */
    @Throws(IOException::class)
    fun buildAudioPiped(ffmpeg: String, sampleRate: Int): Process {
        val cmd = listOf(
            ffmpeg,
            "-hide_banner", "-loglevel", "error", "-nostats",
            "-probesize", "1M", "-analyzeduration", "1000000",
            "-f", "mpegts", "-i", "pipe:0",
            "-vn", "-f", "s16le", "-ar", sampleRate.toString(), "-ac", "2", "-",
        )
        return ProcessBuilder(cmd).start()
    }

    /**
     * Closes the process's output stream and destroys it. Waits up to 1 second for graceful termination, then forcibly destroys if needed.
     * Safe to call multiple times or from any thread. Does nothing if [proc] is null.
     */
    fun gracefulDestroy(proc: Process?) {
        if (proc == null) return
        runCatching { proc.outputStream?.close() }
        proc.destroy()
        runCatching {
            if (!proc.waitFor(1, TimeUnit.SECONDS)) proc.destroyForcibly()
        }.onFailure { e ->
            if (e !is InterruptedException) throw e
            proc.destroyForcibly()
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Builds the common part of the `FFmpeg` command line for both video and audio processes.
     * [alreadyResolved] skips the `MediaHostGuard.resolveSafeUrl` redirect-chain lookup (an HTTP
     * round-trip) for callers that resolved [url] themselves — needed when this runs many times per
     * video (e.g. one scrub-preview sample each) so that cost isn't paid on every single call.
     */
    @Throws(IOException::class)
    private fun baseCommand(
        ffmpeg: String,
        url: String,
        offsetNanos: Long,
        hwAccel: HwAccelBackend,
        alreadyResolved: Boolean = false,
        seekByDecoding: Boolean = false,
    ): MutableList<String> {
        val safeUrl = if (alreadyResolved) url else MediaHostGuard.resolveSafeUrl(url)
        // This container's demuxer cannot be trusted with a jump, so the best available seek is a
        // playlist that already starts at the target ([HlsSeekPlaylist]).
        val trimmed =
            if (seekByDecoding && offsetNanos > 0) HlsSeekPlaylist.trim(safeUrl, offsetNanos) else null
        return inputCommand(ffmpeg, safeUrl, offsetNanos, hwAccel, seekByDecoding, trimmed)
    }

    /**
     * The input half of an `FFmpeg` invocation, with the playlist trim already decided. Split out
     * from [baseCommand] so the argument list can be asserted without a network fetch — the options
     * an input accepts depend on which kind of input it is, and getting that wrong stops the process
     * from starting at all.
     */
    internal fun inputCommand(
        ffmpeg: String,
        safeUrl: String,
        offsetNanos: Long,
        hwAccel: HwAccelBackend,
        seekByDecoding: Boolean,
        trimmed: HlsSeekPlaylist.Trimmed?,
    ): MutableList<String> {
        return mutableListOf<String>().apply {
            add(ffmpeg)
            addAll(listOf("-hide_banner", "-loglevel", "error", "-nostats"))
            addAll(listOf("-protocol_whitelist", "https,tls,tcp,crypto,data,http"))
            if (hwAccel.ffmpegName != null) {
                addAll(listOf("-hwaccel", hwAccel.ffmpegName))
            }
            hwAccel.hwOutputFormat?.let { addAll(listOf("-hwaccel_output_format", it)) }
            if (trimmed == null) {
                addHttpOptions()
            } else {
                // Every option below belongs to the http protocol, and the input here is a data:
                // URI, so none of them has a context to be applied to: FFmpeg reports the first one
                // as "Option headers not found" and refuses to open the input at all. The HLS
                // demuxer's own retry counters are protocol-independent and cover the same ground
                // for the segment fetches, which are the requests that actually matter here.
                addAll(listOf("-seg_max_retry", "3", "-max_reload", "3"))
            }
            addAll(listOf("-rw_timeout", "15000000"))
            // The sources here are plain MP4 / WebM / M4A over HTTPS with stream info in their headers;
            // the default 5 MB / 5 s probe window only postpones the first frame, so keep it tight.
            // Deliberately no -fflags nobuffer: it drops the leading GOP on open and on every
            // in-place seek (see native/lav/src/session.rs, which dropped the same flag for the
            // same reason).
            addAll(listOf("-probesize", "1M", "-analyzeduration", "1000000"))
            when {
                trimmed != null -> {
                    // Forcing the demuxer is required: FFmpeg will not probe a playlist out of a
                    // data: URI ("Not detecting m3u8/hls with non standard extension").
                    addAll(listOf("-f", "hls", "-i", trimmed.url))
                    if (trimmed.residualNanos > 0) addAll(seekArgs(trimmed.residualNanos))
                }
                // -ss after -i makes FFmpeg decode from the start and discard: always correct,
                // and slower the deeper the seek. Only reached when the playlist could not be read.
                seekByDecoding -> {
                    addAll(listOf("-i", safeUrl))
                    if (offsetNanos > 0) addAll(seekArgs(offsetNanos))
                }
                // -ss before -i asks the demuxer to jump, which is what makes a seek instant
                else -> {
                    if (offsetNanos > 0) addAll(seekArgs(offsetNanos))
                    addAll(listOf("-i", safeUrl))
                }
            }
        }
    }

    /** Connection options for an `http(s)` input; see [baseCommand] for why they are conditional. */
    private fun MutableList<String>.addHttpOptions() {
        addAll(listOf("-headers", "User-Agent: $USER_AGENT\r\nReferer: https://www.youtube.com/\r\n"))
        addAll(
            listOf(
                "-reconnect", "1",
                "-reconnect_streamed", "1",
                "-reconnect_delay_max", "10",
                "-reconnect_on_network_error", "1",
                "-reconnect_on_http_error", "5xx",
                // Pull googlevideo over one connection with range requests so it doesn't cut at ~10s
                "-multiple_requests", "1",
            )
        )
    }

    /** `-ss` with [offsetNanos] rendered as seconds. */
    private fun seekArgs(offsetNanos: Long): List<String> =
        listOf("-ss", String.format(Locale.US, "%.6f", offsetNanos / 1e9))
}
