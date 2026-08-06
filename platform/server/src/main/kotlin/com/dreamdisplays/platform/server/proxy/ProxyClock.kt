package com.dreamdisplays.platform.server.proxy

import com.dreamdisplays.core.protocol.proxy.ClockReply
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks this backend's clock offset from the proxy's clock (`proxyEpoch - localEpoch`), estimated
 * NTP-style from [ClockReply] round trips. Used only to translate a proxy-supplied anchor (e.g. a
 * network fullscreen broadcast's start instant) into this backend's local wall clock - never to
 * rewrite [com.dreamdisplays.api.playback.Timeline.serverTimeMs] itself, which must stay a purely
 * local timestamp for the client-side monotonic-staleness guard to keep working.
 *
 * Platform-neutral: holds no `Bukkit` / Minecraft types, so it works unchanged on every backend loader.
 */
object ProxyClock {
    private const val NO_SAMPLE = Long.MIN_VALUE

    /** `proxyEpoch - localEpoch`, EWMA-smoothed (7:1) after the first sample. */
    private val offsetMs = AtomicLong(0L)

    /** Round-trip time of the last accepted sample; used to reject later, worse-than-2x outliers. */
    private val lastRttMs = AtomicLong(NO_SAMPLE)

    /** Feeds one [ClockReply] round trip, sampled against [nowLocal] (this backend's clock at receipt). */
    fun onClockReply(reply: ClockReply, nowLocal: Long) {
        val rtt = nowLocal - reply.backendSendMs
        if (rtt < 0) return // Clock stepped backwards mid-flight; skip rather than poison the average

        val previousRtt = lastRttMs.get()
        if (previousRtt != NO_SAMPLE && rtt >= previousRtt * 2) return // discard latency-spike outliers

        // Midpoint estimate of proxy-vs-local offset, same derivation as classic NTP: average how far
        // ahead the proxy looked on receipt and on send, assuming symmetric one-way latency.
        val sample = ((reply.proxyRecvMs - reply.backendSendMs) + (reply.proxySendMs - nowLocal)) / 2
        val previousOffset = offsetMs.get()
        offsetMs.set(if (previousRtt == NO_SAMPLE) sample else (previousOffset * 7 + sample) / 8)
        lastRttMs.set(rtt)
    }

    /** Translates a proxy-epoch timestamp (e.g. a network broadcast's anchor) to this backend's local clock. */
    fun toLocal(proxyMs: Long): Long = proxyMs - offsetMs.get()

    /** This backend's best current estimate of the proxy's wall clock. */
    fun proxyNow(): Long = System.currentTimeMillis() + offsetMs.get()

    /** Whether at least one [ClockReply] has been accepted yet. */
    fun hasSample(): Boolean = lastRttMs.get() != NO_SAMPLE

    /** Resets to the unsynced state — called when the proxy connection is (re)initialized. */
    fun reset() {
        offsetMs.set(0L)
        lastRttMs.set(NO_SAMPLE)
    }
}
