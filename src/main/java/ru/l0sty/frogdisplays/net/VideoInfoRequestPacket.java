package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VideoInfoRequestPacket(String videoUrl) implements CustomPayload {
    public static final CustomPayload.Id<VideoInfoRequestPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("frogdisplays", "video_info_request"));
    
    public static final PacketCodec<PacketByteBuf, VideoInfoRequestPacket> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, VideoInfoRequestPacket::videoUrl,
                    VideoInfoRequestPacket::new
            ).cast();
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
