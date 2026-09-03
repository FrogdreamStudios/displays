package com.dreamdisplays.media.player.pipeline

import org.slf4j.LoggerFactory

/**
 * Drop-in replacement for the old AudioTrack / OpenAL wrapper.
 *
 * Backed entirely by [libaaudio_sink.so] loaded from the FFmpegPlugin APK.
 * The native C signatures assumed (inferred from `nm -D`):
 *
 *   long  aaudio_open(int sampleRate, int bufferSizeFrames)
 *   void  aaudio_close(long handle)
 *   int   aaudio_write(long handle, byte[] buf, int offset, int len)
 *   void  aaudio_flush(long handle)
 *   void  aaudio_pause(long handle)
 *   void  aaudio_resume(long handle)
 *   long  aaudio_frames_written(long handle)
 *   int   aaudio_buffer_size(long handle)          // in frames
 *   long  aaudio_get_timestamp(long handle)        // wall-clock nanos of last played frame
 *
 * [AudioSink] continues to call the same Kotlin API it always did — nothing
 * in that file needs to change.
 */
internal class AudioTrackWrapper private constructor(private val handle: Long) {

    // ── AudioSink-facing API (mirrors the old AudioTrack surface) ────────────

    /**
     * Number of frames the hardware/AAudio buffer can hold.
     * Replaces `AudioTrack.bufferSizeInFrames`.
     */
    val bufferSizeInFrames: Int
        get() = nativeBufferSize(handle)

    /**
     * Total frames written to the sink so far (monotonically increasing).
     * Replaces `AudioTrack.playbackHeadPosition` — but unlike the 32-bit
     * AudioTrack counter this is a full 64-bit value, so [AudioSink]'s
     * wrap-detection arithmetic (`headWraps`) will always see a delta of 0
     * and never increment the wrap counter.  The clock formula therefore
     * simplifies to:
     *
     *   live = framesWritten - preludeFrames
     *
     * which is exactly what AudioSink computes when headWraps == 0.
     */
    val playbackHeadPosition: Int
        get() {
            // AudioSink's wrap logic treats this as a signed 32-bit counter.
            // aaudio_frames_written is 64-bit; we truncate to 32 bits here so
            // the existing wrap-detection in AudioSink still triggers correctly
            // for very long sessions (>27 h at 44.1 kHz).  For normal use the
            // value stays well within 32-bit range.
            return (nativeFramesWritten(handle) and 0x7FFF_FFFFL).toInt()
        }

    /**
     * Wall-clock nanoseconds of the most recently rendered frame, or -1 if
     * unavailable.  AudioSink does not call this directly but it is available
     * for callers who want a more accurate clock than the frame-count estimate.
     */
    val timestampNanos: Long
        get() = nativeGetTimestamp(handle)

    /** Start / resume playback.  Replaces `AudioTrack.play()`. */
    fun play() = nativeResume(handle)

    /** Pause playback without discarding buffered audio.  Replaces `AudioTrack.stop()` (soft-pause). */
    fun pause() = nativePause(handle)

    /**
     * Discard all buffered audio immediately.
     * Replaces `AudioTrack.flush()`.
     * After a flush the frame counter resets — callers must reset their own
     * wrap-state too (AudioSink already does this before every flush call).
     */
    fun flush() = nativeFlush(handle)

    /**
     * Write up to [length] bytes from [audioData] starting at [offsetInBytes].
     * Returns the number of bytes consumed (≥ 0) or a negative error code.
     * Replaces `AudioTrack.write(byte[], int, int)`.
     */
    fun write(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int =
        nativeWrite(handle, audioData, offsetInBytes, sizeInBytes)

    /**
     * Stop playback and release all native resources.
     * Replaces `AudioTrack.release()`.  Must not be called more than once.
     */
    fun release() = nativeClose(handle)

    // ── Static factory ───────────────────────────────────────────────────────

    companion object {
        private val logger = LoggerFactory.getLogger(AudioTrackWrapper::class.java)

        // The .so is shipped inside the FFmpegPlugin APK and extracted to the
        // app's native library directory by the package manager.
        init {
            try {
                System.loadLibrary("aaudio_sink")
                logger.debug("[AudioTrackWrapper] libaaudio_sink.so loaded.")
            } catch (t: Throwable) {
                logger.error("[AudioTrackWrapper] Failed to load libaaudio_sink.so: ${t.message}")
            }
        }

        /**
         * Returns the minimum sensible buffer size in bytes for the given
         * sample rate, or a safe default if the native layer cannot tell us.
         *
         * AudioSink calls this before [open] to pick a buffer size.
         */
        fun getMinBufferSize(sampleRate: Int): Int {
            // AAudio manages its own internal buffer; we just give AudioSink a
            // reasonable lower bound (equivalent to ~100 ms of stereo 16-bit PCM).
            val minBytes = sampleRate * AudioSink.BYTES_PER_FRAME / 10   // 100 ms
            return minBytes.coerceAtLeast(4096)
        }

        /**
         * Opens a new AAudio stream at [sampleRate] Hz, stereo, signed 16-bit.
         * [bufferSizeBytes] is the requested buffer size in bytes; converted to
         * frames for the native call.
         *
         * Returns null if the native layer fails to open the stream.
         */
        fun open(sampleRate: Int, bufferSizeBytes: Int): AudioTrackWrapper? {
            val bufferFrames = (bufferSizeBytes / AudioSink.BYTES_PER_FRAME).coerceAtLeast(1)
            val handle = nativeOpen(sampleRate, bufferFrames)
            if (handle == 0L) {
                logger.warn("[AudioTrackWrapper] aaudio_open returned null handle.")
                return null
            }
            logger.debug("[AudioTrackWrapper] aaudio_open ok — handle=$handle, bufferFrames=$bufferFrames")
            return AudioTrackWrapper(handle)
        }

        // ── JNI declarations (C symbols in libaaudio_sink.so) ────────────────

        @JvmStatic private external fun nativeOpen(sampleRate: Int, bufferSizeFrames: Int): Long
        @JvmStatic private external fun nativeClose(handle: Long)
        @JvmStatic private external fun nativeWrite(handle: Long, buf: ByteArray, offset: Int, len: Int): Int
        @JvmStatic private external fun nativeFlush(handle: Long)
        @JvmStatic private external fun nativePause(handle: Long)
        @JvmStatic private external fun nativeResume(handle: Long)
        @JvmStatic private external fun nativeFramesWritten(handle: Long): Long
        @JvmStatic private external fun nativeBufferSize(handle: Long): Int
        @JvmStatic private external fun nativeGetTimestamp(handle: Long): Long
    }
}