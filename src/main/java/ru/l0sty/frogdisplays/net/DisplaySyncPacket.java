package ru.l0sty.frogdisplays.net;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record DisplaySyncPacket(UUID id, int seek) implements CustomPayload {
    public static final CustomPayload.Id<DisplaySyncPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("frogdisplays", "display_sync"));

    public static final PacketCodec<PacketByteBuf, DisplaySyncPacket> PACKET_CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, DisplaySyncPacket::id,
                    PacketCodecs.VAR_INT, DisplaySyncPacket::seek,
                    DisplaySyncPacket::new
            ).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
