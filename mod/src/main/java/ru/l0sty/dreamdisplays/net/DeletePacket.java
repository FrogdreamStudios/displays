package ru.l0sty.dreamdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import ru.l0sty.dreamdisplays.PlatformlessInitializer;

import java.util.UUID;

///  Packet for deleting a display.
/// This packet is sent from the client to the server to delete a display with the given ID.
/// @param id the ID of the display to delete.
public record DeletePacket(UUID id) implements CustomPayload {
    public static final Id<DeletePacket> PACKET_ID =
            new Id<>(Identifier.of(PlatformlessInitializer.MOD_ID, "delete"));

    public static final PacketCodec<PacketByteBuf, DeletePacket> PACKET_CODEC =
            PacketCodec.ofStatic(
                    (buf, packet) -> Uuids.PACKET_CODEC.encode(buf, packet.id()),
                    (buf) -> {
                        UUID id = Uuids.PACKET_CODEC.decode(buf);
                        return new DeletePacket(id);
                    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
