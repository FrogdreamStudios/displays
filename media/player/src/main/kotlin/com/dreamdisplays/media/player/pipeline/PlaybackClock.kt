package com.dreamdisplays.media.player.pipeline

/**
 * Playback position as a seek origin plus the wall time elapsed since the first frame. Thread-safe
 * for reads.
 *
 * The origin is deliberately read-only from the outside. Assigning it on a *running* clock shifts
 * the reported position by the whole elapsed interval instead of replacing it (elapsed time is
 * measured from [startWallNanos], so a new origin stacks on top of it), which is never what a caller
 * means. Use [reset] (stop and re-origin), [rebaseTo] (keep running, re-origin as of now) or
 * [moveTo] (whichever of the two matches the current state).
 */
internal class PlaybackClock {
    companion object {
        private const val NOT_STARTED = Long.MIN_VALUE
    }

    @Volatile
    private var originNanosValue = 0L

    @Volatile
    private var startWallNanos = NOT_STARTED

    /** The seek origin — the position [currentTime] reports while the clock is not running. */
    val originNanos: Long get() = originNanosValue

    val isRunning: Boolean get() = startWallNanos != NOT_STARTED

    /** Returns the current playback position in nanos, based on the seek origin and elapsed wall time. */
    fun currentTime(): Long {
        val start = startWallNanos
        return if (start == NOT_STARTED) originNanosValue
        else originNanosValue + (System.nanoTime() - start)
    }

    /** Called once by the video thread when the first frame arrives. */
    fun markFirstFrame() {
        if (startWallNanos == NOT_STARTED) startWallNanos = System.nanoTime()
    }

    /**
     * Shifts the wall-clock origin forward by [nanos] so a paused (parked) interval is excluded from
     * elapsed time — used on un-park so [currentTime] resumes from where it froze instead of jumping
     * ahead by the time the display sat dormant. No-op when the clock isn't running.
     */
    fun addPausedDuration(nanos: Long) {
        if (startWallNanos != NOT_STARTED && nanos > 0) startWallNanos += nanos
    }

    /** Resets the clock to a new seek position (pauses the wall clock). */
    fun reset(offsetNanos: Long) {
        originNanosValue = offsetNanos
        startWallNanos = NOT_STARTED
    }

    /**
     * Atomically re-anchors a *running* clock to [offsetNanos] as of now — used at the replay -> live
     * handoff so the wall clock and the live audio (offset at the same position) agree without a gap.
     * Unlike [reset] the clock stays running, so pacing never momentarily reads "not started".
     */
    fun rebaseTo(offsetNanos: Long) {
        originNanosValue = offsetNanos
        startWallNanos = System.nanoTime()
    }

    /**
     * Moves the reported position to [offsetNanos] whatever the current state: a running clock is
     * rebased (so [currentTime] returns exactly [offsetNanos] right now and keeps advancing), a
     * stopped one is re-originned. This is what callers that merely want to "correct the position"
     * need — assigning the origin directly used to double-count the elapsed interval on a running
     * clock, jumping the position (and everything paced against it) forward by minutes.
     */
    fun moveTo(offsetNanos: Long) {
        if (isRunning) rebaseTo(offsetNanos) else reset(offsetNanos)
    }
}
