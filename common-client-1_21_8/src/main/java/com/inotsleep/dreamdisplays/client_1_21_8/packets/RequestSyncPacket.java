package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Packet for requesting a display sync.
 * This packet is sent from the client to the server to request a sync of the display with the given ID.
 * The server will respond with a SyncPacket containing the display data.
 * We use this packet to ensure that the client has the latest display data after a display has been created, modified, or deleted.
 * This feature is important while using the synchronization feature and, of course, for other cases when the client needs to be updated with the latest display data.
 * @param id the ID of the display to sync.
 */
public record RequestSyncPacket(UUID id) implements CustomPacketPayload, PacketCodec<RequestSyncPacket> {
    public static final Type<RequestSyncPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "req_sync"));

    public static final StreamCodec<FriendlyByteBuf, RequestSyncPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeUUID(packet.id()),
                    (buf) -> {
                        UUID id = buf.readUUID();
                        return new RequestSyncPacket(id);
                    });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    @Override
    public StreamCodec<FriendlyByteBuf, RequestSyncPacket> getCodec() {
        return PACKET_CODEC;
    }

    @Override
    public Class<RequestSyncPacket> getPayloadClass() {
        return RequestSyncPacket.class;
    }
}
