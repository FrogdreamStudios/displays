package com.dreamdisplays.media.player.managers

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Watches whether frames are arriving. If no frame arrives within the applicable threshold,
 * calls [onStall] and resets.
 *
 * A session that has never delivered a frame is held to the much shorter [startupThresholdMs]. Those
 * two failures look identical from here but are not the same thing: a running stream that goes quiet
 * usually recovers on its own, and cutting it off early costs a needless restart, whereas a session
 * that never started is almost always pointed at something dead (a live playlist whose window has
 * moved on, an expired signed URL) and will never recover no matter how long it is given. Cold starts
 * measured well under 5 s even on a 1080p HLS ladder, so [startupThresholdMs] leaves ample headroom.
 */
internal class StreamWatchdog(
    private val debugLabel: String,
    private val isSessionActive: () -> Boolean,
    private val getLastFrameNanos: () -> Long,
    private val stallThresholdMs: Long = 45_000L,
    private val startupThresholdMs: Long = 20_000L,
    private val checkIntervalMs: Long = 1_000L,
    private val onStall: () -> Unit,
) {
    private val logger = LoggerFactory.getLogger("DreamDisplays/StreamWatchdog")

    /** Watchdog task. */
    @Volatile
    private var job: Job? = null

    /**
     * Whether any frame has arrived since [start], and the stamp that decided it. Derived from the
     * stamp moving rather than from a callback, so it holds for every way a session can begin —
     * cold start, in-place seek, quality handoff, reappearance bridge.
     */
    private var deliveredAFrame = false
    private var lastSeenStamp = 0L

    /** Coroutine scope for the watchdog task. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("MediaPlayer-watchdog"))

    /** Start watchdog. */
    fun start() {
        stop()
        deliveredAFrame = false
        lastSeenStamp = getLastFrameNanos()
        job = scope.launch {
            delay(checkIntervalMs)
            // Stops at the first stall: recovery is the caller's job, and it restarts the watchdog
            while (isActive && check()) {
                delay(checkIntervalMs)
            }
        }
    }

    /** Stop watchdog. */
    fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * One pass over [stallThresholdMs] / [startupThresholdMs]. Returns false once [onStall] has been
     * called, which ends the watch.
     *
     * Reporting the same stall on every tick is not harmless. Recovery takes a moment — a re-resolve
     * and a fresh `FFmpeg` — and during that moment the session still has no frames, so a watchdog
     * that keeps counting keeps asking for another recovery, each one tearing down the attempt before
     * it can produce its first frame. That is a restart storm that never converges, and the display
     * simply never plays.
     */
    private fun check(): Boolean {
        return runCatching {
            if (!isSessionActive()) return true
            val stamp = getLastFrameNanos()
            if (stamp != lastSeenStamp) {
                lastSeenStamp = stamp
                deliveredAFrame = true
            }
            val silenceMs = (System.nanoTime() - stamp) / 1_000_000L
            if (silenceMs < (if (deliveredAFrame) stallThresholdMs else startupThresholdMs)) return true
            val what = if (deliveredAFrame) "No frames for $silenceMs ms" else "No first frame after $silenceMs ms"
            logger.warn("$debugLabel $what. Restarting...")
            onStall()
            false
        }.getOrDefault(true)
    }
}
