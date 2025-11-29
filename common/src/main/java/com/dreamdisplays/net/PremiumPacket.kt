package com.dreamdisplays.net

import com.dreamdisplays.Initializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import org.jspecify.annotations.NullMarked

/**
 * Packet for premium status (1080p+ quality, etc.)
 */
@NullMarked
@JvmRecord
data class PremiumPacket(val premium: Boolean) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return PACKET_ID
    }

    companion object {
        @JvmField
        val PACKET_ID: CustomPacketPayload.Type<PremiumPacket> = CustomPacketPayload.Type<PremiumPacket>(
            Identifier.fromNamespaceAndPath(
                Initializer.MOD_ID, "premium"
            )
        )

        @JvmField
        val PACKET_CODEC: StreamCodec<FriendlyByteBuf, PremiumPacket> = StreamCodec.of<FriendlyByteBuf, PremiumPacket>(
            { buf: FriendlyByteBuf?, packet: PremiumPacket? ->
                ByteBufCodecs.BOOL.encode(
                    buf!!,
                    packet!!.premium
                )
            },
            { buf: FriendlyByteBuf? ->
                val premium = ByteBufCodecs.BOOL.decode(buf!!)
                PremiumPacket(premium)
            })
    }
}
