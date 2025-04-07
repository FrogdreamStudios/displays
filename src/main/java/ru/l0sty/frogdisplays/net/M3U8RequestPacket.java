package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record M3U8RequestPacket(String videoUrl, String quality) implements CustomPayload {
    public static final CustomPayload.Id<M3U8RequestPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("frogdisplays", "m3u8_request"));
    
    public static final PacketCodec<PacketByteBuf, M3U8RequestPacket> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, M3U8RequestPacket::videoUrl,
                    PacketCodecs.STRING, M3U8RequestPacket::quality,
                    M3U8RequestPacket::new
            ).cast();
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
