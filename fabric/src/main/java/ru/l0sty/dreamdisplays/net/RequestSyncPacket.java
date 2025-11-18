package ru.l0sty.dreamdisplays.net;

import ru.l0sty.dreamdisplays.PlatformlessInitializer;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet for requesting a display sync.
 * This packet is sent from the client to the server to request a sync of the display with the given ID.
 * The server will respond with a SyncPacket containing the display data.
 * We use this packet to ensure that the client has the latest display data after a display has been created, modified, or deleted.
 * This feature is important while using the synchronization feature and, of course, for other cases when the client needs to be updated with the latest display data.
 * @param id the ID of the display to sync.
 */
public record RequestSyncPacket(UUID id) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestSyncPacket> PACKET_ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PlatformlessInitializer.MOD_ID, "req_sync"));

    public static final StreamCodec<FriendlyByteBuf, RequestSyncPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> UUIDUtil.STREAM_CODEC.encode(buf, packet.id()),
                    (buf) -> {
                        UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
                        return new RequestSyncPacket(id);
                    });

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}