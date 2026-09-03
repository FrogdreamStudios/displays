package com.dreamdisplays.media.player.process

import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A fake [Process] backed by JavaCV's [FFmpegFrameGrabber].
 * Replaces the external ffmpeg binary on platforms where executing
 * binaries is not possible (e.g. Android / noexec storage).
 *
 * Video: writes PPM frames (P6) to [inputStream] — identical wire
 * format to fmpeg -f image2pipe -c:v ppm.
 * Audio: writes raw S16LE stereo PCM — identical to fmpeg -f s16le.
 */
internal class JavaCVProcess private constructor(
    private val grabber: FFmpegFrameGrabber,
    private val pipedIn: PipedInputStream,
    private val pipedOut: PipedOutputStream,
    private val readerThread: Thread,
    private val stopped: AtomicBoolean,
) : Process() {

    private val stderrPipe = PipedInputStream()
    private val stderrOut = PipedOutputStream(stderrPipe)

    override fun getInputStream(): InputStream = pipedIn
    override fun getErrorStream(): InputStream = stderrPipe
    override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()

    override fun waitFor(): Int {
        readerThread.join()
        return if (stopped.get()) 1 else 0
    }

    override fun exitValue(): Int {
        if (readerThread.isAlive) throw IllegalThreadStateException("Process not finished")
        return if (stopped.get()) 1 else 0
    }

    override fun destroy() {
        stopped.set(true)
        readerThread.interrupt()
        runCatching { grabber.stop() }
        runCatching { pipedOut.close() }
        runCatching { stderrOut.close() }
    }

    override fun destroyForcibly(): Process { destroy(); return this }

    override fun isAlive(): Boolean = readerThread.isAlive

    companion object {
        private val logger = LoggerFactory.getLogger(JavaCVProcess::class.java)

        /** Sentinel ffmpeg path that triggers the JavaCV path instead of ProcessBuilder. */
        const val SENTINEL = "javacv"

        /** True when running on Android (Class only exists there). */
        val isAndroid: Boolean = true

        /**
         * Parses the ffmpeg argv built by [MediaProcess] and starts a JavaCV session.
         * Returns null if the args cannot be parsed or the grabber fails to open.
         */
        fun start(args: List<String>, w: Int, h: Int, isAudio: Boolean): JavaCVProcess? {
            // Extract URL — last -i argument
            val url = args.zipWithNext()
                .lastOrNull { it.first == "-i" }?.second ?: run {
                logger.error("JavaCVProcess: no -i argument found in args")
                return null
            }

            // Extract seek offset from -ss (seconds as string)
            val seekSecs = args.zipWithNext()
                .lastOrNull { it.first == "-ss" }?.second?.toDoubleOrNull() ?: 0.0
            val seekMicros = (seekSecs * 1_000_000).toLong()

            return runCatching {
                avutil.av_log_set_level(avutil.AV_LOG_ERROR)

                val grabber = FFmpegFrameGrabber(url).apply {
                    if (!isAudio) {
                        imageWidth = w
                        imageHeight = h
                        pixelFormat = avutil.AV_PIX_FMT_RGB24
                    }
                    sampleRate = if (isAudio) 44100 else 0
                    audioChannels = if (isAudio) 2 else 0
                }
                grabber.start()
                if (seekMicros > 0) grabber.setTimestamp(seekMicros, true)

                val pipedOut = PipedOutputStream()
                val pipedIn = PipedInputStream(pipedOut, 1024 * 1024)
                val stopped = AtomicBoolean(false)

                val thread = Thread({
                    try {
                        val out = BufferedOutputStream(pipedOut, 256 * 1024)
                        if (isAudio) runAudioLoop(grabber, out, stopped)
                        else runVideoLoop(grabber, out, w, h, stopped)
                        out.flush()
                    } catch (_: IOException) {
                        // Pipe closed = consumer stopped, normal shutdown
                    } catch (e: Exception) {
                        logger.warn("JavaCVProcess reader error", e)
                    } finally {
                        runCatching { grabber.stop() }
                        runCatching { pipedOut.close() }
                        stopped.set(true)
                    }
                }, "JavaCV-reader").apply { isDaemon = true }

                val proc = JavaCVProcess(grabber, pipedIn, pipedOut, thread, stopped)
                thread.start()
                proc
            }.getOrElse { e ->
                logger.error("JavaCVProcess failed to start for ", e)
                null
            }
        }

        /**
         * Writes PPM P6 frames to [out].
         * Header: "P6\n<w> <h>\n255\n" followed by raw RGB24 bytes.
         * This is the exact wire format [VideoFramePipe] expects.
         */
        private fun runVideoLoop(
            grabber: FFmpegFrameGrabber,
            out: OutputStream,
            w: Int,
            h: Int,
            stopped: AtomicBoolean,
        ) {
            while (!stopped.get() && !Thread.currentThread().isInterrupted) {
                val frame = grabber.grabImage() ?: break
                if (frame.type != Frame.Type.VIDEO) continue
                val image = frame.image ?: continue
                if (image.isEmpty()) continue

                val header = "P6\n \n255\n".toByteArray(Charsets.US_ASCII)
                out.write(header)

                val buf = image[0] as? ByteBuffer ?: continue
                buf.rewind()
                val bytes = ByteArray(buf.remaining())
                buf.get(bytes)
                out.write(bytes)
            }
        }

        /**
         * Writes raw S16LE stereo PCM to [out].
         * Matches fmpeg -f s16le -ar 44100 -ac 2 which [AudioSink] expects.
         */
        private fun runAudioLoop(
            grabber: FFmpegFrameGrabber,
            out: OutputStream,
            stopped: AtomicBoolean,
        ) {
            while (!stopped.get() && !Thread.currentThread().isInterrupted) {
                val frame = grabber.grabSamples() ?: break
                if (frame.type != Frame.Type.AUDIO) continue
                val samples = frame.samples ?: continue
                if (samples.isEmpty()) continue

                val buf = samples[0] as? ShortBuffer ?: continue
                buf.rewind()
                val tmp = ByteArray(2)
                while (buf.hasRemaining() && !stopped.get()) {
                    val s = buf.get()
                    tmp[0] = (s.toInt() and 0xFF).toByte()
                    tmp[1] = ((s.toInt() shr 8) and 0xFF).toByte()
                    out.write(tmp)
                }
            }
        }
    }
}
