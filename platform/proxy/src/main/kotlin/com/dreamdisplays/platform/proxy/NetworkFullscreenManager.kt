package com.dreamdisplays.platform.proxy

import com.dreamdisplays.core.protocol.proxy.ApplyFullscreen
import com.dreamdisplays.core.protocol.proxy.NetworkSessionInfo
import com.dreamdisplays.core.protocol.proxy.StartNetworkFullscreen
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Proxy-side authority for network-wide fullscreen broadcasts: mints the shared `sessionId` and
 * anchor instant every backend applies identically, resolves the `server <name>|global` scope
 * against the known backend roster, and tracks per-backend acknowledgement / reach so a backend
 * that was empty (no players to ride the plugin message, or none to target) can be retried once it
 * has someone online.
 *
 * Deliberately platform-API-free, like [NetworkBackendRegistry] — both the
 * `Velocity` and `Bungee` adapters drive this with plain strings and do the actual I / O themselves.
 */
object NetworkFullscreenManager {
    /**
     * How far into the future (from the moment the proxy fans a broadcast out) the shared timeline
     * anchor is set. Must comfortably exceed plugin-message fan-out latency plus each backend's
     * media prebuffer, or slower backends would start visibly behind rather than synchronized.
     */
    private const val FANOUT_LEAD_MS = 1_500L

    /** One live network fullscreen broadcast. */
    class Session(
        val sessionId: String,
        val scope: String,
        val ownerId: String,
        val url: String,
        val mode: Int,
        val forced: Boolean,
        val volume: Float,
        val loop: Boolean,
        val quality: String,
        val title: String,
        val anchorProxyMs: Long,
    ) {
        /** Backend name -> last reported reach (player count actually targeted there). */
        val reach: MutableMap<String, Int> = ConcurrentHashMap()

        /** Backends this session is still owed an [ApplyFullscreen] retry for (empty when last tried, or never acked). */
        val pendingServers: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    /** Starts a new network session from a backend's forwarded [request], anchored [FANOUT_LEAD_MS] into the future. */
    fun start(request: StartNetworkFullscreen, proxyNowMs: Long): Session {
        val session = Session(
            sessionId = generateSessionId(),
            scope = request.scope,
            ownerId = request.ownerId,
            url = request.url,
            mode = request.mode,
            forced = request.forced,
            volume = request.volume,
            loop = request.loop,
            quality = request.quality,
            title = request.title,
            anchorProxyMs = proxyNowMs + FANOUT_LEAD_MS,
        )
        sessions[session.sessionId] = session
        return session
    }

    /** Builds the [ApplyFullscreen] every target backend receives for [session]. */
    fun toApplyPacket(session: Session): ApplyFullscreen = ApplyFullscreen(
        sessionId = session.sessionId,
        anchorProxyMs = session.anchorProxyMs,
        ownerId = session.ownerId,
        url = session.url,
        mode = session.mode,
        forced = session.forced,
        volume = session.volume,
        loop = session.loop,
        quality = session.quality,
        title = session.title,
    )

    /** Resolves a `server` flag's [scope] ("global", or one backend name) against [knownServers]. */
    fun targetServers(scope: String, knownServers: Set<String>): Set<String> =
        if (scope.equals("global", ignoreCase = true)) knownServers
        else knownServers.filterTo(mutableSetOf()) { it.equals(scope, ignoreCase = true) }

    /** Marks [servers] as owed a retry of [sessionId]'s [ApplyFullscreen] (called right after fan-out). */
    fun markPending(sessionId: String, servers: Set<String>) {
        sessions[sessionId]?.pendingServers?.addAll(servers)
    }

    /** Records a backend's [NetworkFullscreenAck][com.dreamdisplays.core.protocol.proxy.NetworkFullscreenAck]. */
    fun onAck(sessionId: String, serverName: String, reach: Int, pending: Boolean) {
        val session = sessions[sessionId] ?: return
        session.reach[serverName] = reach
        if (pending) session.pendingServers.add(serverName) else session.pendingServers.remove(serverName)
    }

    /** Live sessions still owed a retry on [serverName] — called when that backend sends a fresh [BackendHello][com.dreamdisplays.core.protocol.proxy.BackendHello]. */
    fun pendingSessionsFor(serverName: String): List<Session> =
        sessions.values.filter { serverName in it.pendingServers }

    /**
     * Ids of every live session that should apply on [serverName] right now — `global`-scope
     * sessions, plus any scoped to this backend by name. Answers a [PlayerReady][com.dreamdisplays.core.protocol.proxy.PlayerReady]
     * with a [ReplayForPlayer][com.dreamdisplays.core.protocol.proxy.ReplayForPlayer] so a player who
     * transfers here keeps seeing what they were already watching.
     */
    fun sessionIdsApplicableTo(serverName: String, knownServers: Set<String>): List<String> =
        sessions.values.filter { serverName in targetServers(it.scope, knownServers) }.map { it.sessionId }

    /** Stops and forgets [sessionId]; returns the removed session, or null if it wasn't live. */
    fun stop(sessionId: String): Session? = sessions.remove(sessionId)

    /** Every live network session, for `/display fullscreen list`. */
    fun list(): List<NetworkSessionInfo> = sessions.values.map {
        NetworkSessionInfo(it.sessionId, it.scope, it.url, it.reach.values.sum())
    }

    /** Whether [sessionId] is a known, currently-live network session. */
    fun isLive(sessionId: String): Boolean = sessions.containsKey(sessionId)

    /** Same 8-character shape as a locally-minted session id, so `stop` suggestions stay uniform. */
    private fun generateSessionId(): String {
        var id: String
        do id = UUID.randomUUID().toString().take(8) while (sessions.containsKey(id))
        return id
    }
}
