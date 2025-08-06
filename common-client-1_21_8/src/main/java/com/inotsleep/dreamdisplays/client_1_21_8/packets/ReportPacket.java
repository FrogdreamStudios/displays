package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Packet for reporting a display.
 * This packet is sent from the client to the server to report about a bad display with the given ID.
 * @param id the ID of the display to report.
 */
public record ReportPacket(UUID id) implements CustomPacketPayload, PacketCodec<ReportPacket> {
    public static final Type<ReportPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "report"));

    public static final StreamCodec<FriendlyByteBuf, ReportPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeUUID(packet.id()),
                    (buf) -> {
                        UUID id = buf.readUUID();
                        return new ReportPacket(id);
                    });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    @Override
    public StreamCodec<FriendlyByteBuf, ReportPacket> getCodec() {
        return PACKET_CODEC;
    }

    @Override
    public CustomPacketPayload.Type<ReportPacket> getType() {
        return PACKET_ID;
    }
}
