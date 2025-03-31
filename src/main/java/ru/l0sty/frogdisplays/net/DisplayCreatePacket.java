package ru.l0sty.frogdisplays.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record DisplayCreatePacket(UUID id, BlockPos pos, BlockPos size, String url) implements CustomPayload {
    public static final CustomPayload.Id<DisplayCreatePacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("frogdisplays", "display_create"));
    public static final PacketCodec<RegistryByteBuf, DisplayCreatePacket> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, DisplayCreatePacket::id,
            BlockPos.PACKET_CODEC, DisplayCreatePacket::pos,
            BlockPos.PACKET_CODEC, DisplayCreatePacket::size,
            PacketCodecs.STRING, DisplayCreatePacket::url,
            DisplayCreatePacket::new
    ).cast();
    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
