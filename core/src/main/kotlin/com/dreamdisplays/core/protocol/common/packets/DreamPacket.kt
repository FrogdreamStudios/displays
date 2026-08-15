package com.dreamdisplays.core.protocol.common.packets

/**
 * Marker for every protocol-v2 packet. The sealed hierarchy exists for exhaustive `when` dispatch
 * in handlers; it is never serialized polymorphically — the wire envelope carries an explicit
 * type id resolved through [com.dreamdisplays.core.protocol.common.PacketRegistry].
 *
 * Lives alongside its implementors (rather than in the parent `protocol` package with the
 * registry) because a `sealed` type's direct subtypes must be declared in the same package.
 */
sealed interface DreamPacket
