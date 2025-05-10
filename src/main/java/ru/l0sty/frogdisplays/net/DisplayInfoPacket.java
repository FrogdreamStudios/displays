package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.joml.Vector3i;
import ru.l0sty.frogdisplays.PlatformlessInitializer;
import ru.l0sty.frogdisplays.util.Facing;

import java.util.UUID;

public record DisplayInfoPacket(UUID id, UUID ownerId, Vector3i pos, int width, int height, String url, Facing facing, boolean isSync, String lang) implements CustomPayload {
    public static final CustomPayload.Id<DisplayInfoPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of(PlatformlessInitializer.MOD_ID, "display_info"));

    public static final PacketCodec<PacketByteBuf, DisplayInfoPacket> PACKET_CODEC =
            PacketCodec.ofStatic(
                    (buf, packet) -> {
                        Uuids.PACKET_CODEC.encode(buf, packet.id());
                        Uuids.PACKET_CODEC.encode(buf, packet.ownerId());

                        PacketCodecs.VAR_INT.encode(buf, packet.pos().x());
                        PacketCodecs.VAR_INT.encode(buf, packet.pos().y());
                        PacketCodecs.VAR_INT.encode(buf, packet.pos().z());

                        PacketCodecs.VAR_INT.encode(buf, packet.width());
                        PacketCodecs.VAR_INT.encode(buf, packet.height());

                        PacketCodecs.STRING.encode(buf, packet.url());

                        PacketCodecs.BYTE.encode(buf, packet.facing().toPacket());
                        PacketCodecs.BOOLEAN.encode(buf, packet.isSync());

                        PacketCodecs.STRING.encode(buf, packet.lang());
                    },
                    (buf) -> {
                        UUID id = Uuids.PACKET_CODEC.decode(buf);
                        UUID ownerId = Uuids.PACKET_CODEC.decode(buf);

                        int x = PacketCodecs.VAR_INT.decode(buf);
                        int y = PacketCodecs.VAR_INT.decode(buf);
                        int z = PacketCodecs.VAR_INT.decode(buf);
                        Vector3i pos = new Vector3i(x, y, z);

                        int width = PacketCodecs.VAR_INT.decode(buf);
                        int height = PacketCodecs.VAR_INT.decode(buf);

                        String url = PacketCodecs.STRING.decode(buf);

                        byte facingByte = PacketCodecs.BYTE.decode(buf);
                        Facing facing = Facing.fromPacket(facingByte);

                        boolean isSync = PacketCodecs.BOOLEAN.decode(buf);
                        String lang = PacketCodecs.STRING.decode(buf);

                        return new DisplayInfoPacket(id, ownerId, pos, width, height, url, facing, isSync, lang);
                    }
            );


    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

