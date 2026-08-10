package com.dreamdisplays.core.protocol.proxy.packets

/**
 * Marker for every packet carried on the `dreamdisplays:proxy` channel between proxy and backend.
 *
 * Lives alongside its implementors (rather than in the parent `proxy` package with the registry)
 * because a `sealed` type's direct subtypes must be declared in the same package.
 */
sealed interface ProxyPacket
