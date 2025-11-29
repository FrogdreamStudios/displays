package com.dreamdisplays.net

import com.dreamdisplays.Initializer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import org.jspecify.annotations.NullMarked
import java.util.*

/**
 * Packet for reporting about an inappropriate display.
 */
@NullMarked
@JvmRecord
data class ReportPacket(val id: UUID) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return PACKET_ID
    }

    companion object {
        @JvmField
        val PACKET_ID: CustomPacketPayload.Type<ReportPacket> = CustomPacketPayload.Type<ReportPacket>(
            Identifier.fromNamespaceAndPath(
                Initializer.MOD_ID, "report"
            )
        )

        @JvmField
        val PACKET_CODEC: StreamCodec<FriendlyByteBuf, ReportPacket> = StreamCodec.of<FriendlyByteBuf, ReportPacket>(
            { buf: FriendlyByteBuf?, packet: ReportPacket? ->
                UUIDUtil.STREAM_CODEC.encode(
                    buf!!,
                    packet!!.id
                )
            },
            { buf: FriendlyByteBuf? ->
                val id = UUIDUtil.STREAM_CODEC.decode(buf!!)
                ReportPacket(id)
            })
    }
}
