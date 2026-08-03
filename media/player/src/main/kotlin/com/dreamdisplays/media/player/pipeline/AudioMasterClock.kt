package com.dreamdisplays.media.player.pipeline

import org.slf4j.LoggerFactory

/**
 * Turns [AudioSink]'s per-session line clock into the single master clock every video pipe paces
 * against.
 */
internal class AudioMasterClock(
    private val debugLabel: String,
    /** Monotonic time source; injectable so the stall / takeover behavior is testable without sleeping. */
    private val nowNanos: () -> Long = System::nanoTime,
) {
    private val logger = LoggerFactory.getLogger("DreamDisplays/AudioMasterClock")

    private companion object {
        /** No epoch seen yet ([AudioSink] epochs start at 1). */
        const val NO_EPOCH = 0

        /**
         * How long the line clock may sit at the same position before the master clock takes over on
         * wall time. Comfortably above the PCM line's own capacity (~0.4 s), so an ordinary underrun
         * rides through untouched and only a genuinely dead clock trips it.
         */
        const val STALL_TAKEOVER_NANOS = 750_000_000L

        /**
         * How far behind the current wall position an exact PTS anchor may pull the pacing clock. A
         * hold this long clears through normal pacing waits; anything more would trip the
         * give-up-and-drop path in [FramePacing] on every queued frame instead.
         */
        const val MAX_BACKWARD_ANCHOR_NANOS = 800_000_000L

        /** Implausible exact-PTS bias bounds: different clocks, a PTS wrap, or a mid-session splice. */
        const val MIN_PLAUSIBLE_EXACT_BIAS_NANOS = -800_000_000L
        const val MAX_PLAUSIBLE_EXACT_BIAS_NANOS = 30_000_000_000L
    }

    private val lock = Any()

    /** [AudioSink.ClockSample.epoch] the current anchor was computed for. */
    private var epoch = NO_EPOCH

    /** Offset added to the raw line clock to put it on the video timeline; 0 for a known origin. */
    private var bias = 0L

    /** Last raw line position seen, and when (wall nanos) it last actually changed. */
    private var lastRaw = 0L
    private var lastAdvanceNanos = 0L

    /** True while the line clock is presumed dead and wall time is driving playback. */
    private var takeover = false
    private var takeoverAnchorWall = 0L
    private var takeoverAnchorOut = 0L

    /** Highest value returned inside the current continuous run; guards against a backwards clock. */
    private var lastOut = Long.MIN_VALUE

    /**
     * Returns the master clock position in content nanos, or -1 when neither the audio line nor the
     * wall clock can say where playback is.
     *
     * @param sample atomic snapshot of the audio line clock (see [AudioSink.sampleClock]).
     * @param wallNanos the wall-clock playback position, or -1 when the wall clock isn't running yet.
     * @param suspended true while the session is deliberately frozen (parked / warm-paused), where a
     * line clock that stops advancing is expected and must not be mistaken for a stall.
     * @param exactBias exact audio-vs-video content offset from shared stream PTS, when both sides
     * have observed theirs; consulted once per session and only for an unknown origin.
     */
    fun nanos(
        sample: AudioSink.ClockSample,
        wallNanos: Long,
        suspended: Boolean,
        exactBias: () -> Long?,
    ): Long {
        if (sample.nanos < 0L) {
            // No line clock at all (starting up, between sessions, bridge prelude): run on wall time
            // and don't let the gap accrue as stall time against whatever session comes back
            synchronized(lock) { lastAdvanceNanos = nowNanos() }
            return wallNanos
        }
        synchronized(lock) {
            val now = nowNanos()
            if (sample.epoch != epoch) beginEpoch(sample, wallNanos, exactBias, now)

            if (sample.nanos != lastRaw) {
                lastRaw = sample.nanos
                lastAdvanceNanos = now
                if (takeover) {
                    takeover = false
                    // Back to the truth
                    lastOut = Long.MIN_VALUE
                    logger.warn("$debugLabel Audio clock recovered; pacing is back on the audio line.")
                }
            } else if (suspended) {
                lastAdvanceNanos = now
            } else if (!takeover && wallNanos >= 0L && now - lastAdvanceNanos >= STALL_TAKEOVER_NANOS) {
                takeover = true
                takeoverAnchorWall = now
                takeoverAnchorOut = if (lastOut == Long.MIN_VALUE) sample.nanos + bias else lastOut
                logger.warn(
                    "$debugLabel Audio clock stuck at ${sample.nanos / 1_000_000} ms for " +
                            "${(now - lastAdvanceNanos) / 1_000_000} ms; pacing video on wall time until it recovers."
                )
            }

            val out =
                if (takeover) takeoverAnchorOut + (now - takeoverAnchorWall)
                else sample.nanos + bias
            if (out > lastOut) lastOut = out
            return lastOut
        }
    }

    /** Forgets all session state; call when the whole playback session is torn down. */
    fun reset() {
        synchronized(lock) {
            epoch = NO_EPOCH
            bias = 0L
            lastRaw = 0L
            lastAdvanceNanos = 0L
            takeover = false
            lastOut = Long.MIN_VALUE
        }
    }

    /** Computes the anchor for a freshly observed audio session. Caller holds [lock]. */
    private fun beginEpoch(
        sample: AudioSink.ClockSample,
        wallNanos: Long,
        exactBias: () -> Long?,
        now: Long,
    ) {
        epoch = sample.epoch
        takeover = false
        lastRaw = sample.nanos
        lastAdvanceNanos = now
        lastOut = Long.MIN_VALUE

        if (sample.originKnown) {
            bias = 0L
            return
        }
        val exact = exactBias()?.takeIf {
            it > MIN_PLAUSIBLE_EXACT_BIAS_NANOS && it < MAX_PLAUSIBLE_EXACT_BIAS_NANOS
        }
        if (exact != null) {
            // Live video can't rewind, so an anchor that would park the clock further behind than
            // pacing can absorb is floored instead of applied literally.
            val floor = (if (wallNanos >= 0L) wallNanos else sample.nanos) - sample.nanos - MAX_BACKWARD_ANCHOR_NANOS
            bias = maxOf(exact, floor)
            logger.debug(
                "$debugLabel A/V anchored by stream PTS: audio joined ${exact / 1_000_000} ms " +
                        "${if (exact >= 0) "ahead of" else "behind"} the video join" +
                        if (bias != exact) " (floored by ${(bias - exact) / 1_000_000} ms: live video can't rewind)."
                        else "."
            )
            return
        }
        bias = if (wallNanos >= 0L) wallNanos - sample.nanos else 0L
        logger.debug(
            "$debugLabel Audio session joined at an unknown content offset; " +
                    "wall-anchored by ${bias / 1_000_000} ms."
        )
    }
}
