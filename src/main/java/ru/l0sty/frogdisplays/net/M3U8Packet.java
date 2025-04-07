package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record M3U8Packet(String url,String videoUrl, String audioUrl, String quality) implements CustomPayload {
    public static final CustomPayload.Id<M3U8Packet> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("frogdisplays", "m3u8_data"));

    public static final PacketCodec<PacketByteBuf, M3U8Packet> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, M3U8Packet::url,
                    PacketCodecs.STRING, M3U8Packet::videoUrl,
                    PacketCodecs.STRING, M3U8Packet::audioUrl,
                    PacketCodecs.STRING, M3U8Packet::quality,
                    M3U8Packet::new
            ).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
