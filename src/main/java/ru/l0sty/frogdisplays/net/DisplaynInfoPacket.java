package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3i;
import ru.l0sty.frogdisplays.utils.Facing;

import java.util.UUID;

public record DisplaynInfoPacket(UUID id, Vector3i pos, int width, int height, String url, Facing facing) implements CustomPayload {
    public static final CustomPayload.Id<DisplaynInfoPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("frogdisplays", "display_info"));

    public static final PacketCodec<PacketByteBuf, DisplaynInfoPacket> PACKET_CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, DisplaynInfoPacket::id,

                    PacketCodec.tuple(
                            PacketCodecs.VAR_INT, Vector3i::x,
                            PacketCodecs.VAR_INT, Vector3i::y,
                            PacketCodecs.VAR_INT, Vector3i::z,
                            Vector3i::new
                    ), DisplaynInfoPacket::pos,

                    PacketCodecs.VAR_INT, DisplaynInfoPacket::width,
                    PacketCodecs.VAR_INT, DisplaynInfoPacket::height,

                    PacketCodecs.STRING, DisplaynInfoPacket::url,

                    PacketCodec.tuple(
                            PacketCodecs.BYTE, Facing::toPacket,
                            Facing::fromPacket
                    ), DisplaynInfoPacket::facing,

                    DisplaynInfoPacket::new
            ).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

