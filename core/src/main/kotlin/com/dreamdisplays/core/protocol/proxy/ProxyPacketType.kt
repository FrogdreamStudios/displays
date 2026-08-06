package com.dreamdisplays.core.protocol.proxy

import kotlin.reflect.KClass

/**
 * Append-only `dreamdisplays:proxy` packet type ids carried by [ProxyEnvelope.type]. A disjoint id
 * space from [com.dreamdisplays.core.protocol.ProtocolPacketType] — the two channels never mix, so
 * starting back at 1 here is intentional, not a collision.
 *
 * These ids are part of the wire protocol. Never reuse or renumber existing entries; only append.
 */
enum class ProxyPacketType(
    val id: Int,
    val packetClass: KClass<out ProxyPacket>,
    val direction: ProxyPacketDirection,
) {
    BACKEND_HELLO(1, BackendHello::class, ProxyPacketDirection.BACKEND_TO_PROXY),
    PROXY_WELCOME(2, ProxyWelcome::class, ProxyPacketDirection.PROXY_TO_BACKEND),
    CLOCK_PROBE(3, ClockProbe::class, ProxyPacketDirection.BACKEND_TO_PROXY),
    CLOCK_REPLY(4, ClockReply::class, ProxyPacketDirection.PROXY_TO_BACKEND),
    START_NETWORK_FULLSCREEN(5, StartNetworkFullscreen::class, ProxyPacketDirection.BACKEND_TO_PROXY),
    APPLY_FULLSCREEN(6, ApplyFullscreen::class, ProxyPacketDirection.PROXY_TO_BACKEND),
    STOP_NETWORK_FULLSCREEN(7, StopNetworkFullscreen::class, ProxyPacketDirection.BIDIRECTIONAL),
    NETWORK_FULLSCREEN_ACK(8, NetworkFullscreenAck::class, ProxyPacketDirection.BACKEND_TO_PROXY),
    LIST_NETWORK_SESSIONS(9, ListNetworkSessions::class, ProxyPacketDirection.BACKEND_TO_PROXY),
    NETWORK_SESSION_LIST(10, NetworkSessionList::class, ProxyPacketDirection.PROXY_TO_BACKEND),
    PLAYER_READY(11, PlayerReady::class, ProxyPacketDirection.BACKEND_TO_PROXY),
    REPLAY_FOR_PLAYER(12, ReplayForPlayer::class, ProxyPacketDirection.PROXY_TO_BACKEND),
    PLAYER_TRANSFERRING(13, PlayerTransferring::class, ProxyPacketDirection.PROXY_TO_BACKEND),
    PLAYER_LEFT_NETWORK(14, PlayerLeftNetwork::class, ProxyPacketDirection.PROXY_TO_BACKEND);

    companion object {
        private val byId = entries.associateBy { it.id }

        init {
            require(byId.size == entries.size) { "Duplicate proxy packet type ids." }
        }

        fun fromId(id: Int): ProxyPacketType? = byId[id]
    }
}
