package com.dreamdisplays.core.protocol.proxy

/**
 * Direction a `dreamdisplays:proxy` packet travels (registry metadata, never serialized).
 * Separate from [com.dreamdisplays.api.protocol.model.PacketDirection] which handles `dreamdisplays:v2`.
 */
enum class ProxyPacketDirection {
    /** Sent by a backend server, handled on the proxy. */
    BACKEND_TO_PROXY,

    /** Sent by the proxy, handled on a backend server. */
    PROXY_TO_BACKEND,

    /** Valid in both directions. */
    BIDIRECTIONAL,
}
