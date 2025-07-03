package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import ru.l0sty.frogdisplays.PlatformlessInitializer;

import java.util.UUID;

/**
 * Packet for requesting a display sync.
 * This packet is sent from the client to the server to request a sync of the display with the given ID.
 * The server will respond with a SyncPacket containing the display data.
 * We use this packet to ensure that the client has the latest display data after a display has been created, modified, or deleted.
 * This feature is important while using the synchronization feature and, of course, for other cases when the client needs to be updated with the latest display data.
 * @param id the ID of the display to sync.
 */
public record RequestSyncPacket(UUID id) implements CustomPayload {
    public static final CustomPayload.Id<RequestSyncPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of(PlatformlessInitializer.MOD_ID, "req_sync"));

    public static final PacketCodec<PacketByteBuf, RequestSyncPacket> PACKET_CODEC =
            PacketCodec.ofStatic(
                    (buf, packet) -> Uuids.PACKET_CODEC.encode(buf, packet.id()),
                    (buf) -> {
                        UUID id = Uuids.PACKET_CODEC.decode(buf);
                        return new RequestSyncPacket(id);
                    });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}