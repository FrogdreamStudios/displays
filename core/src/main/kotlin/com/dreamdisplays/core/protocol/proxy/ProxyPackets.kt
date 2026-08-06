@file:OptIn(ExperimentalSerializationApi::class)

package com.dreamdisplays.core.protocol.proxy

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Backend -> proxy: announces this backend on its first player join (its outbound plugin-message
 * queue only drains once someone is actually connected to ride the message on).
 */
@Serializable
data class BackendHello(
    @ProtoNumber(1) val pluginVersion: String = "",
    @ProtoNumber(2) val mcVersion: String = "",
    @ProtoNumber(3) val platform: String = "",
) : ProxyPacket

/**
 * Proxy -> backend: answers [BackendHello] with the backend's own configured server name (so the
 * backend never needs a `server_name` config key of its own) and the full current roster of
 * backend names in the network, refreshed whenever the roster changes.
 */
@Serializable
data class ProxyWelcome(
    @ProtoNumber(1) val yourServerName: String = "",
    @ProtoNumber(2) val allServerNames: List<String> = emptyList(),
    @ProtoNumber(3) val proxyNowMs: Long = 0L,
) : ProxyPacket

/**
 * Backend -> proxy: NTP-style clock probe, carrying only the backend's own send timestamp. Echoed
 * back by [ClockReply] so the backend can estimate proxy-vs-local clock offset and round-trip time.
 */
@Serializable
data class ClockProbe(
    @ProtoNumber(1) val backendSendMs: Long = 0L,
) : ProxyPacket

/**
 * Proxy -> backend: answers [ClockProbe], echoing [backendSendMs] back alongside the proxy's own
 * receive/send timestamps — the three points a backend needs to estimate one-way offset the usual
 * NTP way, without assuming symmetric latency.
 */
@Serializable
data class ClockReply(
    @ProtoNumber(1) val backendSendMs: Long = 0L,
    @ProtoNumber(2) val proxyRecvMs: Long = 0L,
    @ProtoNumber(3) val proxySendMs: Long = 0L,
) : ProxyPacket

/**
 * Backend -> proxy: forwards a `/display fullscreen start server <scope> ...` command. [scope] is
 * either `global` (every backend) or one specific backend name, already validated client-side by
 * [ProxyWelcome]-driven tab-complete but re-checked by the proxy since that roster can lag.
 * `mode` is the [com.dreamdisplays.api.playback.FullscreenMode] ordinal, wired as a raw `Int` the
 * same way every other protocol packet carries it (see `Packets.kt`) to keep `:core` decoupled from
 * needing `:api` for wire types.
 */
@Serializable
data class StartNetworkFullscreen(
    @ProtoNumber(1) val scope: String = "",
    @ProtoNumber(2) val ownerId: String = "",
    @ProtoNumber(3) val url: String = "",
    @ProtoNumber(4) val mode: Int = 0,
    @ProtoNumber(5) val forced: Boolean = false,
    @ProtoNumber(6) val volume: Float = -1f,
    @ProtoNumber(7) val loop: Boolean = false,
    @ProtoNumber(8) val quality: String = "",
    @ProtoNumber(9) val title: String = "",
) : ProxyPacket

/**
 * Proxy -> backend: fan-out of a [StartNetworkFullscreen] to one target backend. [sessionId] is
 * shared verbatim across every backend in the broadcast (so `/display fullscreen stop <id>` and
 * `list` line up network-wide); [anchorProxyMs] is the proxy's wall-clock instant the broadcast
 * should appear synchronized from - each backend translates it to local time via
 * [com.dreamdisplays.platform.server.proxy.ProxyClock.toLocal] before handing it to
 * [com.dreamdisplays.api.playback.Timeline.start], never by rewriting the timeline's own local
 * `serverTimeMs` semantics.
 */
@Serializable
data class ApplyFullscreen(
    @ProtoNumber(1) val sessionId: String = "",
    @ProtoNumber(2) val anchorProxyMs: Long = 0L,
    @ProtoNumber(3) val ownerId: String = "",
    @ProtoNumber(4) val url: String = "",
    @ProtoNumber(5) val mode: Int = 0,
    @ProtoNumber(6) val forced: Boolean = false,
    @ProtoNumber(7) val volume: Float = -1f,
    @ProtoNumber(8) val loop: Boolean = false,
    @ProtoNumber(9) val quality: String = "",
    @ProtoNumber(10) val title: String = "",
) : ProxyPacket

/**
 * Bidirectional: a backend requests a network session be stopped everywhere (from
 * `/display fullscreen stop <id>` on a session it doesn't own locally), or the proxy fans that stop
 * back out to every backend hosting it - same wire shape either way, direction tells them apart.
 */
@Serializable
data class StopNetworkFullscreen(
    @ProtoNumber(1) val sessionId: String = "",
) : ProxyPacket

/**
 * Backend -> proxy: reports the outcome of applying an [ApplyFullscreen]. [pending] is true when the
 * backend had zero online players to target - not an error, just "nothing to acknowledge yet"; the
 * proxy retries this backend's [ApplyFullscreen] on its next [BackendHello].
 */
@Serializable
data class NetworkFullscreenAck(
    @ProtoNumber(1) val sessionId: String = "",
    @ProtoNumber(2) val reach: Int = 0,
    @ProtoNumber(3) val pending: Boolean = false,
) : ProxyPacket

/** Backend -> proxy: requests the current network-wide fullscreen roster, for `/display fullscreen list`. */
@Serializable
data class ListNetworkSessions(
    @ProtoNumber(1) val unused: Int = 0,
) : ProxyPacket

/** One network session's aggregate state, as reported in a [NetworkSessionList]. */
@Serializable
data class NetworkSessionInfo(
    @ProtoNumber(1) val sessionId: String = "",
    @ProtoNumber(2) val scope: String = "",
    @ProtoNumber(3) val url: String = "",
    @ProtoNumber(4) val totalReach: Int = 0,
)

/** Proxy -> backend: answers [ListNetworkSessions] with every live network fullscreen session. */
@Serializable
data class NetworkSessionList(
    @ProtoNumber(1) val sessions: List<NetworkSessionInfo> = emptyList(),
) : ProxyPacket

/**
 * Backend -> proxy: a player finished the `dreamdisplays:v2` handshake on this backend — sent on
 * every join and every proxy-driven server switch, not just the once-per-restart [BackendHello].
 * The proxy answers with a [ReplayForPlayer] so network state (currently: which live network
 * fullscreen sessions apply here) survives a `/server` switch without the player noticing.
 */
@Serializable
data class PlayerReady(
    @ProtoNumber(1) val playerId: String = "",
) : ProxyPacket

/** Proxy -> backend: replays live network fullscreen session ids that [playerId] should be shown here. */
@Serializable
data class ReplayForPlayer(
    @ProtoNumber(1) val playerId: String = "",
    @ProtoNumber(2) val sessionIds: List<String> = emptyList(),
) : ProxyPacket

/**
 * Proxy -> backend: tells the backend [playerId] is currently on ([from]) that a switch to [to] is
 * starting, sent while the player's connection to [from] is still open (`Velocity`
 * `ServerPreConnectEvent` / `Bungee` `ServerConnectEvent` both fire before the origin disconnects) -
 * lets that backend distinguish "player is transferring" from "player quit" for anything gated on
 * `PlayerQuitEvent` (currently: the watch-party host-disconnect grace timer).
 */
@Serializable
data class PlayerTransferring(
    @ProtoNumber(1) val playerId: String = "",
    @ProtoNumber(2) val from: String = "",
    @ProtoNumber(3) val to: String = "",
) : ProxyPacket

/**
 * Proxy -> backend: [playerId] actually left the whole network (proxy-level disconnect, not a
 * backend switch) - clears any [PlayerTransferring] suppression still armed for them on [server], so
 * a transfer attempt that was started but never completed doesn't permanently mask a real quit.
 */
@Serializable
data class PlayerLeftNetwork(
    @ProtoNumber(1) val playerId: String = "",
    @ProtoNumber(2) val server: String = "",
) : ProxyPacket
