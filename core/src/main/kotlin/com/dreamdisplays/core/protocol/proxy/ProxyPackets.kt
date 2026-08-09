@file:OptIn(ExperimentalSerializationApi::class)

package com.dreamdisplays.core.protocol.proxy

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Backend -> proxy: announces backend on first player join. */
@Serializable
data class BackendHello(
    @ProtoNumber(1) val pluginVersion: String = "",
    @ProtoNumber(2) val mcVersion: String = "",
    @ProtoNumber(3) val platform: String = "",
) : ProxyPacket

/** Proxy -> backend: answers [BackendHello] with server name and backend roster. */
@Serializable
data class ProxyWelcome(
    @ProtoNumber(1) val yourServerName: String = "",
    @ProtoNumber(2) val allServerNames: List<String> = emptyList(),
    @ProtoNumber(3) val proxyNowMs: Long = 0L,
) : ProxyPacket

/** Backend -> proxy: NTP-style clock probe for offset / latency estimation. */
@Serializable
data class ClockProbe(
    @ProtoNumber(1) val backendSendMs: Long = 0L,
) : ProxyPacket

/** Proxy -> backend: answers [ClockProbe] with offset estimation points. */
@Serializable
data class ClockReply(
    @ProtoNumber(1) val backendSendMs: Long = 0L,
    @ProtoNumber(2) val proxyRecvMs: Long = 0L,
    @ProtoNumber(3) val proxySendMs: Long = 0L,
) : ProxyPacket

/** Backend -> proxy: forward `/display fullscreen start` command. */
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
    @ProtoNumber(10) val targetsRaw: String = "",
) : ProxyPacket

/** Proxy -> backend: fan-out of [StartNetworkFullscreen] with sessionId for sync. */
@Serializable
data class ApplyFullscreen(
    @ProtoNumber(1) val sessionId: String = "",
    @ProtoNumber(2) val anchorProxyMs: Long = 0L,
    @ProtoNumber(11) val sharedDisplayId: String = "",
    @ProtoNumber(3) val ownerId: String = "",
    @ProtoNumber(4) val url: String = "",
    @ProtoNumber(5) val mode: Int = 0,
    @ProtoNumber(6) val forced: Boolean = false,
    @ProtoNumber(7) val volume: Float = -1f,
    @ProtoNumber(8) val loop: Boolean = false,
    @ProtoNumber(9) val quality: String = "",
    @ProtoNumber(10) val title: String = "",
    @ProtoNumber(12) val targetsRaw: String = "",
) : ProxyPacket

/** Bidirectional: stop network fullscreen everywhere. */
@Serializable
data class StopNetworkFullscreen(
    @ProtoNumber(1) val sessionId: String = "",
) : ProxyPacket

/** Backend -> proxy: report outcome of [ApplyFullscreen]. */
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

/** Backend -> proxy: player finished v2 handshake, sent on every join / server switch. */
@Serializable
data class PlayerReady(
    @ProtoNumber(1) val playerId: String = "",
) : ProxyPacket

/** Proxy -> backend: replay network fullscreen sessions for [playerId]. */
@Serializable
data class ReplayForPlayer(
    @ProtoNumber(1) val playerId: String = "",
    @ProtoNumber(2) val sessionIds: List<String> = emptyList(),
    @ProtoNumber(3) val minimizedSessionIds: List<String> = emptyList(),
) : ProxyPacket

/** Proxy -> backend: player transferring from [from] to [to]. */
@Serializable
data class PlayerTransferring(
    @ProtoNumber(1) val playerId: String = "",
    @ProtoNumber(2) val from: String = "",
    @ProtoNumber(3) val to: String = "",
) : ProxyPacket

/** Proxy -> backend: [playerId] left the network (not a switch). */
@Serializable
data class PlayerLeftNetwork(
    @ProtoNumber(1) val playerId: String = "",
    @ProtoNumber(2) val server: String = "",
) : ProxyPacket

/** Backend -> proxy: host requests network-wide watch party. */
@Serializable
data class StartNetworkWatchParty(
    @ProtoNumber(1) val hostId: String = "",
    @ProtoNumber(2) val url: String = "",
    @ProtoNumber(3) val lang: String = "",
) : ProxyPacket

/** Proxy -> backend: answer [StartNetworkWatchParty] with [sharedDisplayId]. */
@Serializable
data class ApplyNetworkWatchParty(
    @ProtoNumber(1) val partyId: String = "",
    @ProtoNumber(2) val sharedDisplayId: String = "",
    @ProtoNumber(3) val hostId: String = "",
    @ProtoNumber(4) val url: String = "",
    @ProtoNumber(5) val lang: String = "",
) : ProxyPacket

/** Proxy -> backend: register [playerId] as member of party [partyId]. */
@Serializable
data class JoinNetworkWatchParty(
    @ProtoNumber(1) val partyId: String = "",
    @ProtoNumber(2) val sharedDisplayId: String = "",
    @ProtoNumber(3) val playerId: String = "",
    @ProtoNumber(4) val hostId: String = "",
    @ProtoNumber(5) val url: String = "",
    @ProtoNumber(6) val lang: String = "",
) : ProxyPacket

/** Bidirectional: network watch party state; mirrors [com.dreamdisplays.core.protocol.WatchPartyState]. */
@Serializable
data class NetworkWatchPartyState(
    @ProtoNumber(1) val partyId: String = "",
    @ProtoNumber(2) val sharedDisplayId: String = "",
    @ProtoNumber(3) val state: Int = 0,
    @ProtoNumber(4) val hostId: String = "",
    @ProtoNumber(5) val hostName: String = "",
    @ProtoNumber(6) val url: String = "",
    @ProtoNumber(7) val lang: String = "",
    @ProtoNumber(8) val readyCount: Int = 0,
    @ProtoNumber(9) val nearbyCount: Int = 0,
    @ProtoNumber(10) val countdownStartEpochMs: Long = 0,
    @ProtoNumber(11) val positionMs: Long = 0,
    @ProtoNumber(12) val serverTimeMs: Long = 0,
    @ProtoNumber(13) val durationMs: Long = 0,
    @ProtoNumber(14) val paused: Boolean = true,
) : ProxyPacket

/**
 * Bidirectional: tears a network watch party down everywhere - a backend requests it (host closed
 * the party locally), or the proxy fans it back out to every backend with members, same as
 * [StopNetworkFullscreen].
 */
@Serializable
data class CloseNetworkWatchParty(
    @ProtoNumber(1) val partyId: String = "",
) : ProxyPacket

/** Asks network what video the display named by [token] is playing (full id or 8-char abbreviation). */
@Serializable
data class ResolveDisplayToken(
    @ProtoNumber(1) val requestId: String = "",
    @ProtoNumber(2) val token: String = "",
    @ProtoNumber(3) val originServer: String = "",
) : ProxyPacket

/** Answers [ResolveDisplayToken] from the backend hosting the display, with current video URL. */
@Serializable
data class DisplayTokenResolved(
    @ProtoNumber(1) val requestId: String = "",
    @ProtoNumber(2) val originServer: String = "",
    @ProtoNumber(3) val url: String = "",
) : ProxyPacket

/** Backend -> proxy: viewer collapsed/restored network fullscreen to/from PiP. */
@Serializable
data class PlayerFullscreenMinimized(
    @ProtoNumber(1) val sessionId: String = "",
    @ProtoNumber(2) val playerId: String = "",
    @ProtoNumber(3) val minimized: Boolean = false,
) : ProxyPacket

/** One display a backend hosts, as advertised in a [BackendDisplayIndex]. */
@Serializable
data class DisplayIndexEntry(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val url: String = "",
)

/** Backend advertises all displays it hosts, enabling proxy to answer [ResolveDisplayToken] queries. */
@Serializable
data class BackendDisplayIndex(
    @ProtoNumber(1) val displays: List<DisplayIndexEntry> = emptyList(),
) : ProxyPacket
