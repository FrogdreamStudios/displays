package com.dreamdisplays.media.player.process

import com.dreamdisplays.api.security.policy.MediaHosts
import com.dreamdisplays.media.player.pipeline.VideoFramePipe
import com.dreamdisplays.media.runtime.security.MediaHostGuard
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
     * The JVM PPM path has to scale and upload every frame at the display texture's resolution. On
     * Android launchers where MediaCodec is unavailable, 720p at 30 FPS overwhelms that path even
     * for a low-resolution source. Keep the audio-driven timeline stable by reducing only the
     * output cadence; users can override it with `-Ddreamdisplays.android.maxFps=<1..60>`.
     */
    private val ANDROID_MAX_OUTPUT_FPS: Double? = null

    /** Sanitizes source frame rate to constant pipeline rate. Raw pipes carry no frame-rate metadata. */
    fun outputFps(sourceFps: Double?): Double =
        (sourceFps?.takeIf { it.isFinite() && it > 1.0 && it <= 240.0 } ?: DEFAULT_OUTPUT_FPS)
            .let { fps -> ANDROID_MAX_OUTPUT_FPS?.let { minOf(fps, it) } ?: fps }

    /** True when [stderr] is `FFmpeg` reporting that the input simply has no audio track, rather than any kind of real failure. */
    fun indicatesNoAudioStream(stderr: String): Boolean {
        return stderr.contains("does not contain any stream") && NO_AUDIO_DISQUALIFIERS.none { stderr.contains(it, ignoreCase = true) }
    }

    /** Markers that mean the empty output came from a failed input, not from a silent one. */
    private val NO_AUDIO_DISQUALIFIERS = listOf(
        "Server returned", "Connection refused", "Connection timed out", "Invalid data found",
        "No such file or directory", "Protocol not found", "Immediate exit requested",
        "Error opening input", "Unknown error", "I/O error", "HTTP error",
    )

    /**
     * Request headers for [url]. Platform CDNs get the `Referer` some of them insist on (see
     * [MediaHosts.refererFor]); a host a player pasted gets a plain browser identity and nothing
     * else, so its operator learns no more from the request than any visitor would give them.
     */
    private fun headerArgs(url: String): List<String> {
        val referer = MediaHosts.refererFor(url)?.let { "Referer: $it\r\n" }.orEmpty()
        return listOf("-headers", "User-Agent: $USER_AGENT\r\n$referer")
    }

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

    /** Builds FFmpeg process to read video frames from [url] at offset, scaled and cropped to [w]x[h]. */
    @Throws(IOException::class)
    fun buildVideo(
        ffmpeg: String, url: String, w: Int, h: Int, offsetNanos: Long, hwAccel: HwAccelBackend, fps: Double,
        alreadyResolved: Boolean = false, seekByDecoding: Boolean = false,
    ): Process {
        val args = videoArgs(ffmpeg, url, w, h, offsetNanos, hwAccel, VideoTransport.PPM, fps, alreadyResolved, seekByDecoding)
        if (ffmpeg == JavaCVProcess.SENTINEL)
            return JavaCVProcess.start(args, w, h, isAudio = false) ?: throw IOException("JavaCV video session failed to start")
        return ProcessBuilder(args).start()
    }

    /** Builds full FFmpeg argv for video session emitting [transport] on stdout. Used by native pipeline directly. */
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

    /** Builds FFmpeg process that seeks to offset in URL and writes a single letterboxed JPEG frame. */
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
        if (ffmpeg == JavaCVProcess.SENTINEL) return JavaCVProcess.start(cmd, w, h, isAudio = false) ?: throw IOException("JavaCV frame extract session failed to start")
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
        if (ffmpeg == JavaCVProcess.SENTINEL) return JavaCVProcess.start(cmd, 0, 0, isAudio = true) ?: throw IOException("JavaCV audio session failed to start")
        return ProcessBuilder(cmd).start()
    }

    /** Builds `FFmpeg` process that decodes audio from MPEG-TS piped to stdin. */
    @Throws(IOException::class)
    fun buildAudioPiped(ffmpeg: String, sampleRate: Int): Process {
        val cmd = listOf(
            ffmpeg,
            "-hide_banner", "-loglevel", "error", "-nostats",
            "-probesize", "1M", "-analyzeduration", "1000000",
            "-f", "mpegts", "-i", "pipe:0",
            "-vn", "-f", "s16le", "-ar", sampleRate.toString(), "-ac", "2", "-",
        )
        if (ffmpeg == JavaCVProcess.SENTINEL) return JavaCVProcess.start(cmd, 0, 0, isAudio = true) ?: throw IOException("JavaCV audio piped session failed to start")
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

    /** Builds common `FFmpeg` command line for video and audio. [alreadyResolved] skips SSRF guard. */
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
        val trimmed =
            if (seekByDecoding && offsetNanos > 0) HlsSeekPlaylist.trim(safeUrl, offsetNanos) else null
        return inputCommand(ffmpeg, safeUrl, offsetNanos, hwAccel, seekByDecoding, trimmed)
    }

    /** Input half of `FFmpeg` invocation with playlist trim already decided. */
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
                println("[MediaProcess] Initializing FFmpeg with hardware acceleration: ${hwAccel.name} (codec: ${hwAccel.ffmpegName})")
                addAll(listOf("-hwaccel", hwAccel.ffmpegName))
            } else {
                println("[MediaProcess] Initializing FFmpeg with software decoding (NONE)")
            }
            hwAccel.hwOutputFormat?.let { addAll(listOf("-hwaccel_output_format", it)) }
            if (trimmed == null) {
                addHttpOptions(safeUrl)
            } else {
                addAll(listOf("-seg_max_retry", "3", "-max_reload", "3"))
            }
            addAll(listOf("-rw_timeout", "15000000"))
            addAll(listOf("-probesize", "1M", "-analyzeduration", "1000000"))
            when {
                trimmed != null -> {
                    addAll(listOf("-f", "hls", "-i", trimmed.url))
                    if (trimmed.residualNanos > 0) addAll(seekArgs(trimmed.residualNanos))
                }
                seekByDecoding -> {
                    addAll(listOf("-i", safeUrl))
                    if (offsetNanos > 0) addAll(seekArgs(offsetNanos))
                }
                else -> {
                    if (offsetNanos > 0) addAll(seekArgs(offsetNanos))
                    addAll(listOf("-i", safeUrl))
                }
            }
        }
    }

    /** Connection options for an `http(s)` input. */
    private fun MutableList<String>.addHttpOptions(url: String) {
        addAll(headerArgs(url))
        addAll(
            listOf(
                "-reconnect", "1",
                "-reconnect_streamed", "1",
                "-reconnect_delay_max", "10",
                "-reconnect_on_network_error", "1",
                "-reconnect_on_http_error", "5xx",
                "-multiple_requests", "1",
            )
        )
    }

    /** `-ss` with [offsetNanos] rendered as seconds. */
    private fun seekArgs(offsetNanos: Long): List<String> =
        listOf("-ss", String.format(Locale.US, "%.6f", offsetNanos / 1e9))
}