package ru.l0sty.frogdisplays.net;

import com.mojang.serialization.Codec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record VideoInfoPacket(String url, int duration, List<String> qualities) implements CustomPayload {
    public static final CustomPayload.Id<VideoInfoPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("frogdisplays", "video_info"));

    public static final PacketCodec<PacketByteBuf, VideoInfoPacket> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, VideoInfoPacket::url,
                    PacketCodecs.VAR_INT, VideoInfoPacket::duration,
                    PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), VideoInfoPacket::qualities,
                    VideoInfoPacket::new
            ).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
