package com.dreamdisplays.media.player.pipeline

import com.dreamdisplays.api.media.audio.AudioDspStage
import com.dreamdisplays.media.player.MediaPlayer
import com.dreamdisplays.media.player.pipeline.AudioSink.Companion.LINE_BUFFER_BYTES
import com.dreamdisplays.media.player.pipeline.AudioSink.Companion.MIN_PACE_BYTES
import com.dreamdisplays.media.player.pipeline.AudioSink.Companion.PCM_RING_MAX_BYTES
import com.dreamdisplays.media.player.util.MediaBufferEffects
import com.dreamdisplays.media.player.util.MediaUtil
import com.dreamdisplays.media.player.util.daemon
import kotlinx.io.IOException
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*

/**
 * Manages the `javax.sound` PCM pipeline for one `FFmpeg` audio process, and owns the audio master
 * clock every video pipe paces against ([sampleClock]).
 */
internal class AudioSink(private val debugLabel: String) {
    /** Logger. */
    private val logger = LoggerFactory.getLogger("DreamDisplays/AudioSink")

    companion object {
        /** The PCM sample rate used by every session: 44.1 kHz, stereo, signed 16-bit little-endian. */
        const val SAMPLE_RATE = 44100

        /** Stereo 16-bit PCM: 4 bytes per frame. One second is SAMPLE_RATE * BYTES_PER_FRAME bytes. */
        const val BYTES_PER_FRAME = 4

        /** Chunk size for each read from the audio process and each write to the line. 1 / 20 s of stereo 16-bit PCM */
        private const val CHUNK_BYTES = SAMPLE_RATE * 2 * 2 / 20

        /**
         * Capacity of the PCM line (8 chunks, ~0.4 s). This is only the ceiling the pacer may grow into,
         * not the steady-state latency: [paceLiveWrite] holds the *unplayed* backlog near
         * [paceTargetBytes] (well below this), so DSP-to-ear latency tracks the adaptive target, while the
         * spare capacity is headroom the target can expand into on a stuttering machine.
         */
        private const val LINE_BUFFER_BYTES = CHUNK_BYTES * 8

        /** Smallest backlog the live pacer holds (~0.05 s) — the floor it settles back to when playback is clean. */
        private const val MIN_PACE_BYTES = CHUNK_BYTES

        /** Largest backlog the pacer will grow to (~0.30 s) before it stops trading latency for stability. */
        private const val MAX_PACE_BYTES = CHUNK_BYTES * 6

        /** Backlog step added per detected underrun / eased off per clean-playback window. */
        private const val PACE_STEP_BYTES = CHUNK_BYTES

        /** Consecutive clean chunks (~20 s at 20 chunks/s) before the pacer eases the target back down one step. */
        private const val PACE_RECOVER_CHUNKS = 400

        /** Number of times to retry opening the audio line if it is temporarily unavailable. */
        private const val OPEN_RETRIES = 3

        /** Retry delay between line-open attempts, multiplied by the attempt number (1..OPEN_RETRIES). */
        private const val RETRY_DELAY_MS = 200L

        /** ~30 s of stereo 16-bit PCM kept for the reappearance audio bridge. */
        private const val PCM_RING_MAX_BYTES = SAMPLE_RATE * BYTES_PER_FRAME * 30

        /** Content gaps below this (~1 video frame) aren't worth a catch-up skip — inaudible as lip sync. */
        private const val CATCHUP_MIN_NANOS = 40_000_000L

        /** Max refine passes of the catch-up skip; each pass shrinks the residual by the decode-speed factor. */
        private const val CATCHUP_MAX_PASSES = 4

        /** Converts a count of stereo 16-bit PCM frames into nanos of content. */
        fun framesToNanos(frames: Long): Long = frames * 1_000_000_000L / SAMPLE_RATE

        /** Converts a byte count of stereo 16-bit PCM into nanos of content. */
        fun bytesToNanos(bytes: Long): Long = framesToNanos(bytes / BYTES_PER_FRAME)
    }

    /**
     * A sample of the audio master clock, taken as one atomic read so the position, the session it
     * belongs to, and the meaning of that position can never come from different sessions.
     *
     * @property nanos content position the listener is hearing right now, or -1 when no line clock is
     * available (no session, line not opened, no PCM flowing yet, or a bridge still on its prelude).
     * @property epoch identity of the session; a change means the line restarted from frame 0 and any
     * anchoring computed against the previous one is void.
     * @property originKnown true when [nanos] is a real content position (the process was seeked to a
     * known offset), false when the session joined the stream wherever the server handed it the first
     * sample (live HLS) and the value is only meaningful as a *rate*, needing an external anchor.
     */
    class ClockSample(
        @JvmField val nanos: Long,
        @JvmField val epoch: Int,
        @JvmField val originKnown: Boolean,
    ) {
        companion object {
            /** No line clock available. */
            val NONE = ClockSample(-1L, 0, false)
        }
    }

    /**
     * Clock-relevant state of one audio session. Mutated only by the thread that owns the session
     * (plus [stop] / park control), and published as a single reference through [current].
     *
     * @property contentStartNanos content position of live line frame 0, advanced by a catch-up skip
     * before the line is ever started.
     * @property preludeFrames frames of cached prelude played ahead of the live PCM on a bridge line
     * (0 for a normal session), subtracted so the clock stays continuous across the cached -> live seam.
     */
    private class LineSession(
        @JvmField val epoch: Int,
        @JvmField val originKnown: Boolean,
        @JvmField val preludeFrames: Long,
        contentStartNanos: Long,
    ) {
        @Volatile
        @JvmField
        var contentStartNanos: Long = contentStartNanos

        @Volatile
        @JvmField
        var line: SourceDataLine? = null

        /** True once live PCM has actually been read from the process (see [AudioSink.sampleClock]). */
        @Volatile
        @JvmField
        var pcmFlowing = false

        /** False while a bridge is still playing its cached prelude; see [AudioSink.onBridgeHandoff]. */
        @Volatile
        @JvmField
        var exposeLiveClock = preludeFrames == 0L
    }

    /**
     * Content catch-up spec for an audio session that starts while playback is already running (an
     * audio-track switch, an in-place audio restart): the process was seeked to [contentStartNanos],
     * but by the time its PCM starts flowing the playback clock ([playbackClock]) has moved on — the
     * sink discards exactly that span of leading PCM before playing, so the audible audio joins
     * already aligned with the video instead of permanently lagging by the spawn + connect latency.
     */
    class CatchUp(val contentStartNanos: Long, val playbackClock: () -> Long)

    /** Current volume multiplier applied to each audio chunk. */
    @Volatile
    var currentVolume: Double = 1.0

    /**
     * Optional per-source acoustics DSP stage; when set it replaces [MediaBufferEffects.applyVolumeS16LE]
     * for every chunk (including the bridge prelude), receiving [currentVolume] as its bypass gain.
     */
    @Volatile
    private var dspStage: AudioDspStage? = null

    /** Installs (or clears) the acoustics DSP stage for this session; resets its state immediately. */
    fun setDspStage(stage: AudioDspStage?) {
        dspStage = stage
        stage?.reset()
    }

    /** Adaptive backlog the live pacer currently targets; persists across sessions to keep learning the machine. */
    private var paceTargetBytes = MIN_PACE_BYTES

    /** Consecutive underrun-free chunks since the last target change, used to ease the target back down. */
    private var paceCleanChunks = 0

    /** True once the first live chunk has been written, so an empty line before then isn't read as an underrun. */
    private var paceStreaming = false

    /** Guards session publication so a superseded thread can never install its line over a newer one. */
    private val sessionLock = Any()

    /** The session whose line currently drives the master clock, or null when nothing is open. */
    @Volatile
    private var current: LineSession? = null

    private var epochCounter = 0

    /**
     * Samples the audio master clock. See [ClockSample]; [ClockSample.NONE] while no line clock is
     * available, in which case callers fall back to the wall clock.
     */
    fun sampleClock(): ClockSample {
        val s = current ?: return ClockSample.NONE
        val ln = s.line ?: return ClockSample.NONE
        if (!s.exposeLiveClock || !s.pcmFlowing) return ClockSample.NONE
        val live = ln.longFramePosition - s.preludeFrames
        if (live < 0L) return ClockSample.NONE
        return ClockSample(s.contentStartNanos + framesToNanos(live), s.epoch, s.originKnown)
    }

    /** Gate a bridge session waits on for its live `FFmpeg` process; null outside a bridge. */
    @Volatile
    private var liveGate: CountDownLatch? = null

    @Volatile
    private var liveProc: Process? = null

    /** When set and true, the reader pauses the line (keeping it open) and idles — used to keep the audio
     *  process warm while a display is parked out of render distance. Set once per session; persists across
     *  every audio path (normal start and the reappearance bridge) so any live audio thread observes it. */
    @Volatile
    private var parked: AtomicBoolean? = null

    /** Stderr buffer for the bridge's live process, set alongside [liveProc] by [provideLiveInput] and
     *  read by [runBridge] once it resumes past [awaitLiveInput] — see [drainStderr] for why each
     *  session gets its own buffer instead of a shared instance field. */
    @Volatile
    private var liveStderrBuf: StringBuilder? = null

    /** Installs the session park flag (see [parked]); set once by the owning session manager. */
    fun setParkFlag(flag: AtomicBoolean?) {
        parked = flag
    }

    /**
     * Opens a new clock session, superseding any previous one, and resets the per-session shared
     * state (ring, DSP) that belongs to whatever is now playing.
     */
    private fun beginSession(contentStartNanos: Long, originKnown: Boolean, preludeFrames: Long = 0L): LineSession {
        val session = synchronized(sessionLock) {
            LineSession(++epochCounter, originKnown, preludeFrames, contentStartNanos).also { current = it }
        }
        clearRing()
        dspStage?.reset()
        return session
    }

    /**
     * Publishes [ln] as [session]'s clock source. Returns false when the session has already been
     * superseded (a newer start, or a [stop]) — the caller must then abandon the line instead of
     * letting a dead session drive the master clock.
     */
    private fun publishLine(session: LineSession, ln: SourceDataLine): Boolean {
        synchronized(sessionLock) {
            if (current !== session) return false
            session.line = ln
            return true
        }
    }

    /** True while [session] is the one driving the clock. */
    private fun owns(session: LineSession): Boolean = current === session

    /**
     * Detaches [ln] from [session] and clears the session if it is still current, so the clock reads
     * "unavailable" rather than a frozen last position once its thread has finished.
     */
    private fun retireSession(session: LineSession, ln: SourceDataLine?) {
        synchronized(sessionLock) {
            if (session.line === ln) session.line = null
            if (current === session) current = null
        }
    }

    /** Rolling ring of recently decoded raw PCM (newest last), so a reappearing display can play the
     *  cached-replay window with sound. Capped at [PCM_RING_MAX_BYTES]. Guarded by [pcmRing]'s monitor. */
    private val pcmRing = ArrayDeque<ByteArray>()
    private var pcmRingBytes = 0

    /** Appends a copy of the first [len] bytes of [chunk] to the PCM ring, evicting the oldest over budget. */
    private fun ringPush(chunk: ByteArray, len: Int) {
        if (len <= 0) return
        val copy = chunk.copyOf(len)
        synchronized(pcmRing) {
            pcmRing.addLast(copy)
            pcmRingBytes += len
            while (pcmRingBytes > PCM_RING_MAX_BYTES && pcmRing.size > 1) {
                pcmRingBytes -= pcmRing.removeFirst().size
            }
        }
    }

    /**
     * Drops all cached PCM. Called at the start of every fresh audio session ([beginSession]) so a
     * seek or restart can never leave pre-seek samples in the ring.
     */
    private fun clearRing() {
        synchronized(pcmRing) {
            pcmRing.clear()
            pcmRingBytes = 0
        }
    }

    /**
     * Returns up to the most-recent [maxBytes] of audible cached PCM (oldest-to-newest), or empty when
     * none. PCM still queued in the line (decoded but not yet played) is excluded so the snapshot ends at
     * the heard position — keeping the cached audio aligned with the cached video when replayed.
     */
    fun snapshotPcm(maxBytes: Int): ByteArray {
        if (maxBytes <= 0) return ByteArray(0)
        val unplayed = current?.line?.let { (it.bufferSize - it.available()).coerceAtLeast(0) } ?: 0
        synchronized(pcmRing) {
            // Round both bounds down to a whole stereo 16-bit frame so the slice never starts mid-sample
            // (unplayed comes from the line's byte counters and need not be frame-aligned).
            val end = ((pcmRingBytes - unplayed).coerceAtLeast(0) / BYTES_PER_FRAME) * BYTES_PER_FRAME
            val take = (end.coerceAtMost(maxBytes) / BYTES_PER_FRAME) * BYTES_PER_FRAME
            if (take <= 0) return ByteArray(0)
            val start = end - take
            val out = ByteArray(take)
            var idx = 0      // running byte offset of the current chunk's start
            var written = 0
            for (c in pcmRing) {
                val cStart = idx
                val cEnd = idx + c.size
                if (cEnd > start && cStart < end) {
                    val from = maxOf(start, cStart) - cStart
                    val to = minOf(end, cEnd) - cStart
                    System.arraycopy(c, from, out, written, to - from)
                    written += to - from
                }
                idx = cEnd
                if (idx >= end) break
            }
            // 4-byte (stereo 16-bit) frame alignment so playback never splits a sample.
            val misalign = written % BYTES_PER_FRAME
            return if (misalign == 0 && written == out.size) out else out.copyOfRange(misalign, written)
        }
    }

    /**
     * Starts the audio reading / writing loop.
     * @param contentStartNanos content position the process was seeked to — the origin of this
     * session's master clock (see [ClockSample]).
     * @param originKnown false when the process joined the stream at a point only the server knows
     * (live HLS), so [contentStartNanos] is nominal and pacing must anchor the clock externally.
     * @param onUnexpectedEnd called with the accumulated ffmpeg stderr if the audio process ends on its
     * own (crash, broken pipe, EOF) rather than via [stop] — never called for a deliberate teardown.
     * @param catchUp when set, leading PCM between the process's seek target and the live playback
     * clock is discarded before playback starts (see [CatchUp]).
     */
    fun start(
        proc: Process,
        terminated: AtomicBoolean,
        stopFlag: AtomicBoolean,
        contentStartNanos: Long,
        originKnown: Boolean,
        startGate: CountDownLatch? = null,
        onUnexpectedEnd: (String) -> Unit = {},
        catchUp: CatchUp? = null,
    ): Thread {
        liveGate = null
        val session = beginSession(contentStartNanos, originKnown)
        val stderrBuf = drainStderr(proc, stopFlag)
        return daemon(
            { run(session, proc, terminated, stopFlag, startGate, stderrBuf, onUnexpectedEnd, catchUp) },
            "MediaPlayer-audio",
        ).also { it.start() }
    }

    /**
     * Prewarms a replacement line for a seamless in-place audio-track switch and, once it is primed,
     * flips it in for the currently playing line with no audible gap. Runs entirely on a background
     * thread; the live line is left untouched until the flip, so its clock, ring and pacing stay
     * valid while the replacement opens, applies its [catchUp] skip and pre-buffers leading PCM
     * (silent — not yet started). At the flip the old line is stopped and closed and the new one is
     * promoted and started from its primed buffer (instant sound), then [onPromoted] fires so the
     * caller can tear down the old half's process.
     *
     * The prime window carries volume only (no acoustics DSP) and is not ring-cached, to avoid racing
     * the still-live old session's shared DSP / ring state; both resume once the line is promoted.
     *
     * @param contentStartNanos see [start]; advanced by whatever the [catchUp] skip discards, so the
     * promoted line's clock is exact from its very first sample.
     * @param shouldPromote re-checked once the line is primed, right before the flip; when it returns
     * false (e.g. a newer switch has superseded this one) the flip is skipped and [onAborted] fires.
     * @param onPromoted invoked on the switch thread right after the flip (old line stopped, new one
     * playing) — the caller swaps ownership to the new half here.
     * @param onAborted invoked if the replacement never promotes (line open / prime failed, superseded,
     * or a stop fired first) so the caller can discard the new process and keep the current track.
     * @param onUnexpectedEnd see [start]; armed only once the promoted line begins its normal pump.
     */
    fun startSwitch(
        proc: Process,
        terminated: AtomicBoolean,
        stopFlag: AtomicBoolean,
        contentStartNanos: Long,
        originKnown: Boolean,
        catchUp: CatchUp?,
        shouldPromote: () -> Boolean = { true },
        onPromoted: () -> Unit,
        onAborted: () -> Unit = {},
        onUnexpectedEnd: (String) -> Unit = {},
    ): Thread {
        val stderrBuf = drainStderr(proc, stopFlag)
        return daemon(
            {
                runSwitch(
                    proc,
                    terminated,
                    stopFlag,
                    contentStartNanos,
                    originKnown,
                    catchUp,
                    shouldPromote,
                    onPromoted,
                    onAborted,
                    stderrBuf,
                    onUnexpectedEnd
                )
            },
            "MediaPlayer-audio-switch-line",
        ).also { it.start() }
    }

    /**
     * Background body of [startSwitch]: open + skip + pre-buffer the replacement line while the old
     * one plays, then atomically flip and continue as a normal live pump. See [startSwitch] for the
     * DSP / ring caveats on the prime window.
     */
    private fun runSwitch(
        proc: Process, terminated: AtomicBoolean, stopFlag: AtomicBoolean, contentStartNanos: Long,
        originKnown: Boolean, catchUp: CatchUp?, shouldPromote: () -> Boolean, onPromoted: () -> Unit,
        onAborted: () -> Unit, stderrBuf: StringBuilder, onUnexpectedEnd: (String) -> Unit,
    ) {
        var newLine: SourceDataLine? = null
        var session: LineSession? = null
        var promoted = false
        var pumped = false

        fun isInterrupted() = terminated.get() || stopFlag.get()
        runCatching {
            proc.inputStream.use { input ->
                val fmt = pcmFormat()
                val info = DataLine.Info(SourceDataLine::class.java, fmt)
                if (!AudioSystem.isLineSupported(info)) {
                    logger.warn("$debugLabel PCM line not supported (switch).")
                    return@use
                }
                newLine = openLine(info, fmt) ?: run {
                    logger.warn("$debugLabel [audio] switch line failed to open.")
                    return@use
                }

                if (isInterrupted()) return@use
                val skippedNanos = catchUp?.let { skipCatchUp(input, it, terminated, stopFlag) } ?: 0L

                if (isInterrupted() || !primeSwitchLine(input, newLine, terminated, stopFlag)) return@use
                if (isInterrupted() || !shouldPromote()) return@use

                // The flip publishes the new session and its line together, so no clock reader can ever
                // see the new line paired with the old session's content origin (or vice versa).
                val old = current?.line
                old?.apply {
                    runCatching { stop() }
                    runCatching { flush() }
                }
                val s = beginSession(contentStartNanos + skippedNanos, originKnown)
                session = s
                publishLine(s, newLine)
                newLine.start()
                // Only now may the clock be read: the line is running and its primed buffer is the
                // content at contentStartNanos, so frame position 0 really is that position.
                s.pcmFlowing = true
                old?.let { runCatching { it.close() } }

                promoted = true
                logger.debug("$debugLabel [audio] switch line promoted — new track playing.")
                onPromoted()

                pumpLive(s, input, newLine, terminated, stopFlag)
                pumped = true
            }
        }.onFailure { e ->
            if (isInterrupted()) return@onFailure
            when (e) {
                is IOException -> if (MediaPlayer.DEBUG) logger.warn("$debugLabel Switch read: ${e.message}.")
                else -> logger.warn("$debugLabel Switch pipeline: ${e.message}.")
            }
        }

        newLine?.let { ln ->
            session?.let { retireSession(it, ln) }
            runCatching { ln.flush() }
            runCatching { ln.stop() }
            runCatching { ln.close() }
        }

        if (!promoted) onAborted()

        if (pumped && !isInterrupted()) {
            onUnexpectedEnd(synchronized(stderrBuf) { stderrBuf.toString() })
        }
    }

    /**
     * Pre-buffers the replacement switch line with leading PCM before it is started (silent). Volume
     * is applied but not acoustics DSP, and nothing is ring-cached — the old session still owns that
     * shared state until the flip. Fills up to the pacer's learned backlog (clamped) so the promoted
     * line starts with a healthy buffer and no open-latency dropout. Returns false only when no PCM
     * arrived at all (immediate EOF) or a stop fired before anything was buffered.
     */
    private fun primeSwitchLine(
        input: InputStream, ln: SourceDataLine, terminated: AtomicBoolean, stopFlag: AtomicBoolean,
    ): Boolean {
        val chunk = ByteArray(CHUNK_BYTES)
        val primeTarget = paceTargetBytes.coerceIn(CHUNK_BYTES * 3, LINE_BUFFER_BYTES - CHUNK_BYTES)
        var buffered = 0
        while (buffered < primeTarget && !terminated.get() && !stopFlag.get()) {
            val n = MediaUtil.readFull(input, chunk, CHUNK_BYTES)
            if (n <= 0) return buffered > 0
            MediaBufferEffects.applyVolumeS16LE(chunk, n, currentVolume)
            writeFully(ln, chunk, n, terminated, stopFlag)
            buffered += n
        }
        return buffered > 0
    }

    /**
     * Starts a reappearance bridge on a single line: plays the cached [prelude] immediately, then — on the
     * same [SourceDataLine], with no flush and no second line — continues with the live PCM once
     * [provideLiveInput] supplies its process. The seam is therefore sample-continuous. The master clock
     * stays unavailable until the line crosses out of the prelude, so video pacing is unaffected while it
     * plays.
     * @param liveEdgeNanos content position the live PCM (i.e. the first frame past the prelude) starts at.
     * @param onUnexpectedEnd see [start].
     */
    fun startBridge(
        prelude: ByteArray,
        liveEdgeNanos: Long,
        terminated: AtomicBoolean,
        stopFlag: AtomicBoolean,
        onUnexpectedEnd: (String) -> Unit = {},
    ): Thread {
        liveProc = null
        liveGate = CountDownLatch(1)
        val session = beginSession(
            contentStartNanos = liveEdgeNanos,
            originKnown = true,
            preludeFrames = (prelude.size / BYTES_PER_FRAME).toLong(),
        )
        return daemon(
            { runBridge(session, prelude, terminated, stopFlag, onUnexpectedEnd) },
            "MediaPlayer-audio-bridge"
        ).also { it.start() }
    }

    /** Supplies the live `FFmpeg` audio process to an in-flight bridge session (see [startBridge]). */
    fun provideLiveInput(proc: Process, stopFlag: AtomicBoolean) {
        liveStderrBuf = drainStderr(proc, stopFlag)
        liveProc = proc
        liveGate?.countDown()
    }

    /**
     * Marks the replay -> live handoff: the playback clock has just been re-anchored to the live edge, so
     * the master clock may now report the (live-relative) line clock. Called from the first-live-frame
     * callback, in lock-step with `PlaybackClock.rebaseTo`.
     */
    fun onBridgeHandoff() {
        current?.exposeLiveClock = true
    }

    /** Flushes and closes the audio line immediately, retiring the clock. Safe to call from any thread. */
    fun stop() {
        liveGate?.countDown() // Release a bridge thread still waiting for its live input
        val s = synchronized(sessionLock) { current.also { current = null } } ?: return
        val ln = s.line ?: return
        s.line = null
        runCatching { ln.flush() }
        runCatching { ln.stop() }
        runCatching { ln.close() }
    }

    /** Pauses playback for a warm park without closing or flushing the queued PCM. */
    fun pauseForPark() {
        current?.line?.let { runCatching { it.stop() } }
    }

    /** Resumes a line paused by [pauseForPark]. */
    fun resumeFromPark() {
        current?.line?.let { runCatching { it.start() } }
    }

    /**
     * Runs the audio reading / writing loop until the process ends or [terminated] / [stopFlag] is set.
     * Fires [onUnexpectedEnd] once the pump loop is reached and then ends on its own (neither flag set).
     */
    private fun run(
        session: LineSession, proc: Process, terminated: AtomicBoolean, stopFlag: AtomicBoolean,
        startGate: CountDownLatch?, stderrBuf: StringBuilder, onUnexpectedEnd: (String) -> Unit,
        catchUp: CatchUp? = null,
    ) {
        var ln: SourceDataLine? = null
        var pumped = false
        runCatching {
            proc.inputStream.use { input ->
                val fmt = pcmFormat()
                val info = DataLine.Info(SourceDataLine::class.java, fmt)
                if (!AudioSystem.isLineSupported(info)) {
                    logger.warn("$debugLabel PCM line not supported.")
                    return@runCatching
                }
                ln = openLine(info, fmt) ?: run {
                    logger.warn("$debugLabel [audio] line failed to open.")
                    return@runCatching
                }
                if (terminated.get() || stopFlag.get()) return@runCatching
                logger.debug("$debugLabel [audio] line open, waiting for start gate...")
                if (!awaitStartGate(startGate, terminated, stopFlag)) {
                    logger.debug("$debugLabel [audio] aborted before start gate opened (terminated / stopped).")
                    return@runCatching
                }
                catchUp?.let { session.contentStartNanos += skipCatchUp(input, it, terminated, stopFlag) }
                if (terminated.get() || stopFlag.get()) return@runCatching
                if (!publishLine(session, ln)) {
                    logger.debug("$debugLabel [audio] session superseded before its line started; abandoning it.")
                    return@runCatching
                }
                ln.start()
                logger.debug("$debugLabel [audio] start gate passed, line started — audio is now playing.")
                pumpLive(session, input, ln, terminated, stopFlag)
                pumped = true
            }
        }.onFailure { e ->
            if (!terminated.get() && !stopFlag.get()) {
                if (e is IOException) {
                    if (MediaPlayer.DEBUG) logger.warn("$debugLabel Read: ${e.message}.")
                } else {
                    logger.warn("$debugLabel Pipeline: ${e.message}.")
                }
            }
        }

        ln?.let {
            retireSession(session, it)
            runCatching { it.flush() }
            runCatching { it.stop() }
            runCatching { it.close() }
        }

        if (pumped && !terminated.get() && !stopFlag.get()) {
            onUnexpectedEnd(synchronized(stderrBuf) { stderrBuf.toString() })
        }
    }

    /**
     * Runs a reappearance bridge on one line (see [startBridge]): opens the line, plays the cached
     * [prelude], then continues with the live PCM on the same line once it is attached. The prelude is
     * not ring-cached (it is already-played audio); only the live PCM feeds the rolling ring. Fires
     * [onUnexpectedEnd] once the live PCM pump is reached and then ends on its own (see [run]).
     */
    private fun runBridge(
        session: LineSession, prelude: ByteArray, terminated: AtomicBoolean, stopFlag: AtomicBoolean,
        onUnexpectedEnd: (String) -> Unit,
    ) {
        var ln: SourceDataLine? = null
        var pumped = false
        var stderrBuf: StringBuilder? = null

        runCatching {
            val fmt = pcmFormat()
            val info = DataLine.Info(SourceDataLine::class.java, fmt)
            if (!AudioSystem.isLineSupported(info)) {
                logger.warn("$debugLabel PCM line not supported (bridge).")
                return@runCatching
            }
            ln = openLine(info, fmt) ?: run {
                logger.warn("$debugLabel [audio] bridge line failed to open.")
                return@runCatching
            }
            if (terminated.get() || stopFlag.get()) return@runCatching
            if (!publishLine(session, ln)) {
                logger.debug("$debugLabel [audio] bridge superseded before it started; abandoning it.")
                return@runCatching
            }
            ln.start()
            val cachedSec = prelude.size / (SAMPLE_RATE * BYTES_PER_FRAME).toDouble()
            logger.debug("$debugLabel [audio] bridge line started; playing ${"%.2f".format(cachedSec)} s cached prelude.")

            // 1. Cached prelude — paced naturally by the line (not ring-cached: already-played audio)
            writePrelude(ln, prelude, terminated, stopFlag)

            // 2. Continue with the live PCM on the SAME line: sample-continuous, no flush, no second line
            val proc = awaitLiveInput(terminated, stopFlag) ?: run {
                logger.debug("$debugLabel [audio] bridge ended before live input attached.")
                return@runCatching
            }

            // provideLiveInput sets liveStderrBuf before liveProc, both before opening the gate this
            // just passed, so it is already populated for this bridge's live process here.
            stderrBuf = liveStderrBuf
            logger.debug("$debugLabel [audio] bridge handing off cached -> live on one line.")
            proc.inputStream.use { input -> pumpLive(session, input, ln, terminated, stopFlag) }
            pumped = true
        }.onFailure { e ->
            if (!terminated.get() && !stopFlag.get()) {
                logger.warn("$debugLabel Bridge pipeline: ${e.message}.")
            }
        }

        ln?.let {
            retireSession(session, it)
            runCatching { it.flush() }
            runCatching { it.stop() }
            runCatching { it.close() }
        }

        if (pumped && !terminated.get() && !stopFlag.get()) {
            onUnexpectedEnd(stderrBuf?.let { buf -> synchronized(buf) { buf.toString() } } ?: "")
        }
    }

    /** The PCM line format shared by every session: 44.1 kHz stereo signed 16-bit little-endian. */
    private fun pcmFormat() = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE.toFloat(), 16, 2, 4, SAMPLE_RATE.toFloat(), false,
    )

    /** Writes the cached bridge prelude to [ln] (volume applied), without ring-caching it. */
    private fun writePrelude(
        ln: SourceDataLine,
        prelude: ByteArray,
        terminated: AtomicBoolean,
        stopFlag: AtomicBoolean
    ) {
        val chunk = ByteArray(CHUNK_BYTES)
        var off = 0
        while (off < prelude.size && !terminated.get() && !stopFlag.get()) {
            val n = minOf(CHUNK_BYTES, prelude.size - off)
            System.arraycopy(prelude, off, chunk, 0, n)
            dspStage?.process(chunk, n, currentVolume) ?: MediaBufferEffects.applyVolumeS16LE(chunk, n, currentVolume)
            writeFully(ln, chunk, n, terminated, stopFlag)
            off += n
        }
    }

    /** Polls the bridge's live-input gate until the process is attached, or stop/terminate aborts it. */
    private fun awaitLiveInput(terminated: AtomicBoolean, stopFlag: AtomicBoolean): Process? {
        val gate = liveGate ?: return null
        while (!terminated.get() && !stopFlag.get()) {
            try {
                if (gate.await(20L, TimeUnit.MILLISECONDS)) return liveProc
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt(); return null
            }
        }
        return null
    }

    /**
     * Discards the leading PCM span between [CatchUp.contentStartNanos] and the live playback clock,
     * so the first sample actually played matches the video's current position, and returns how much
     * content (in nanos) was dropped so the caller can advance the session's clock origin by exactly
     * that much. Runs before the line starts playing (nothing here is audible, and none of it is
     * ring-cached — it is never heard). Iterative: each skip pass takes real time to read (the process
     * decodes faster than realtime, so passes converge), and the residual is re-measured against the
     * clock until it is inaudible.
     */
    private fun skipCatchUp(
        input: InputStream, catchUp: CatchUp, terminated: AtomicBoolean, stopFlag: AtomicBoolean,
    ): Long {
        val scratch = ByteArray(CHUNK_BYTES)
        var skippedBytes = 0L
        repeat(CATCHUP_MAX_PASSES) {
            val contentNow = catchUp.contentStartNanos + bytesToNanos(skippedBytes)
            val gapNanos = catchUp.playbackClock() - contentNow
            if (gapNanos < CATCHUP_MIN_NANOS) {
                if (skippedBytes > 0L) {
                    logger.debug(
                        "$debugLabel [audio] catch-up skipped ${bytesToNanos(skippedBytes) / 1_000_000} ms " +
                                "of PCM to join in sync."
                    )
                }
                return bytesToNanos(skippedBytes)
            }
            var toSkip = gapNanos * SAMPLE_RATE / 1_000_000_000L * BYTES_PER_FRAME
            while (toSkip > 0 && !terminated.get() && !stopFlag.get()) {
                val n = MediaUtil.readFull(input, scratch, minOf(CHUNK_BYTES.toLong(), toSkip).toInt())
                if (n <= 0) return bytesToNanos(skippedBytes) // EOF mid-skip: pump loop will report it
                skippedBytes += n
                toSkip -= n
            }
            if (terminated.get() || stopFlag.get()) return bytesToNanos(skippedBytes)
        }
        return bytesToNanos(skippedBytes)
    }

    /** Reads live PCM from [input] and writes it to [ln], ring-caching each chunk for the next bridge. */
    private fun pumpLive(
        session: LineSession, input: InputStream, ln: SourceDataLine,
        terminated: AtomicBoolean, stopFlag: AtomicBoolean,
    ) {
        val chunk = ByteArray(CHUNK_BYTES)
        var firstChunk = true
        var totalBytes = 0L
        // Fresh line for this session: don't let its initially-empty buffer read as an underrun on the
        // first paced write. The learned target itself persists across sessions.
        paceStreaming = false
        paceCleanChunks = 0
        while (!terminated.get() && !stopFlag.get()) {
            if (parkIfRequested(ln, terminated, stopFlag)) break
            val n = MediaUtil.readFull(input, chunk, CHUNK_BYTES)
            if (n <= 0) {
                // A deliberate teardown destroys the process mid-read; that EOF is expected and not
                // worth a warning (during a session-restart loop it reads as a phantom audio crash).
                if (terminated.get() || stopFlag.get()) break
                if (firstChunk) {
                    logger.warn("$debugLabel [audio] no PCM data from ffmpeg (EOF on first read).")
                } else {
                    val seconds = totalBytes / (SAMPLE_RATE * BYTES_PER_FRAME).toDouble()
                    logger.warn("$debugLabel [audio] PCM stream ended after ${"%.1f".format(seconds)} s.")
                }
                break
            }
            totalBytes += n
            if (firstChunk) {
                logger.debug("$debugLabel [audio] first PCM chunk received ($n bytes)."); firstChunk = false
                session.pcmFlowing = true
            }
            val owned = owns(session)
            if (owned) ringPush(chunk, n) // Cache raw PCM (pre-volume) for the reappearance audio bridge
            val dsp = if (owned) dspStage else null
            dsp?.process(chunk, n, currentVolume) ?: MediaBufferEffects.applyVolumeS16LE(chunk, n, currentVolume)
            paceLiveWrite(ln, chunk, n, terminated, stopFlag)
        }
    }

    /**
     * If the session is parked, pauses the line (keeping its buffer + open handle), idles until un-parked,
     * then resumes the line. The audio `FFmpeg` process blocks on pipe back-pressure meanwhile, staying
     * warm. Returns true only when stop/terminate fired while parked (caller should break the loop).
     */
    private fun parkIfRequested(ln: SourceDataLine, terminated: AtomicBoolean, stopFlag: AtomicBoolean): Boolean {
        val pk = parked ?: return false
        if (!pk.get()) return false
        runCatching { ln.stop() } // Pause playback, keep the buffered tail and the line open
        while (pk.get() && !terminated.get() && !stopFlag.get()) {
            try {
                Thread.sleep(20)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt(); return true
            }
        }
        if (terminated.get() || stopFlag.get()) return true
        runCatching { ln.start() } // Resume from exactly where it paused (frame position is continuous)
        return false
    }

    /** Writes the first [n] bytes of [chunk] to [ln], retrying short writes until stop/terminate. */
    private fun writeFully(
        ln: SourceDataLine,
        chunk: ByteArray,
        n: Int,
        terminated: AtomicBoolean,
        stopFlag: AtomicBoolean
    ) {
        var written = 0
        while (written < n && !terminated.get() && !stopFlag.get()) {
            val w = ln.write(chunk, written, n - written)
            if (w <= 0) break
            written += w
        }
    }

    /**
     * Writes one live chunk while keeping the line's *unplayed* backlog near [paceTargetBytes] instead of
     * letting it fill the whole [LINE_BUFFER_BYTES] capacity, so DSP-to-ear latency (and thus how quickly
     * acoustics track head / position changes) stays close to the target rather than the buffer size.
     *
     * The target is self-adapting: if the line drained to empty while this chunk was being produced (an
     * underrun), it grows a step to buy more headroom; after a long clean-playback window it eases back
     * one step toward [MIN_PACE_BYTES]. So it settles at the smallest backlog the machine can actually
     * sustain without stuttering. A / V sync is unaffected — the master clock tracks the line's playout
     * position, not how far ahead we queue.
     */
    private fun paceLiveWrite(
        ln: SourceDataLine,
        chunk: ByteArray,
        n: Int,
        terminated: AtomicBoolean,
        stopFlag: AtomicBoolean
    ) {
        val unplayedBefore = (ln.bufferSize - ln.available()).coerceAtLeast(0)
        if (paceStreaming) {
            if (unplayedBefore == 0) {
                paceTargetBytes = (paceTargetBytes + PACE_STEP_BYTES).coerceAtMost(MAX_PACE_BYTES)
                paceCleanChunks = 0
            } else if (++paceCleanChunks >= PACE_RECOVER_CHUNKS) {
                paceTargetBytes = (paceTargetBytes - PACE_STEP_BYTES).coerceAtLeast(MIN_PACE_BYTES)
                paceCleanChunks = 0
            }
        }
        writeFully(ln, chunk, n, terminated, stopFlag)
        paceStreaming = true
        // Hold the backlog at the target: wait for playout to drain the surplus before feeding the next chunk.
        while (!terminated.get() && !stopFlag.get()) {
            if ((ln.bufferSize - ln.available()).coerceAtLeast(0) <= paceTargetBytes) break
            try {
                Thread.sleep(1)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt(); break
            }
        }
    }

    /** Waits for video to publish its first frame, polling so stop() can interrupt quickly. */
    private fun awaitStartGate(
        gate: CountDownLatch?,
        terminated: AtomicBoolean,
        stopFlag: AtomicBoolean,
    ): Boolean {
        if (gate == null || gate.count == 0L) return true
        while (!terminated.get() && !stopFlag.get()) {
            try {
                if (gate.await(50L, TimeUnit.MILLISECONDS)) return true
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    /**
     * Opens and returns a [SourceDataLine] with the specified format, retrying a few times if the line is temporarily unavailable.
     */
    private fun openLine(info: DataLine.Info, fmt: AudioFormat): SourceDataLine? {
        repeat(OPEN_RETRIES) { attempt ->
            try {
                return (AudioSystem.getLine(info) as SourceDataLine).also { it.open(fmt, LINE_BUFFER_BYTES) }
            } catch (e: LineUnavailableException) {
                if (attempt == OPEN_RETRIES - 1) {
                    logger.warn("$debugLabel Line unavailable: ${e.message}.")
                    return null
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt(); return null
                }
            }
        }
        return null
    }

    /**
     * Drains [proc]'s stderr, logs interesting lines FFmpeg emits as they arrive (it runs at
     * -loglevel error), and accumulates them into a buffer scoped to this call — returned so the
     * caller's session can read it back once its own pump loop ends.
     */
    private fun drainStderr(proc: Process, stopFlag: AtomicBoolean): StringBuilder {
        val buf = StringBuilder()
        daemon({
            try {
                proc.errorStream.bufferedReader().forEachLine { line ->
                    if (line.isNotBlank()) {
                        val trimmed = line.trim()
                        if (stopFlag.get()) {
                            logger.debug("$debugLabel [audio] FFmpeg stderr during teardown: $trimmed.")
                        } else if (MediaUtil.isInterestingStderr(trimmed)) {
                            logger.warn("$debugLabel [audio] FFmpeg stderr: $trimmed.")
                        }
                        synchronized(buf) { buf.append(line).append('\n') }
                    }
                }
            } catch (_: IOException) {
            }
        }, "MediaPlayer-astderr").start()
        return buf
    }
}
