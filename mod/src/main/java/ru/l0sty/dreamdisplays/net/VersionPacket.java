package ru.l0sty.dreamdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.l0sty.dreamdisplays.PlatformlessInitializer;

/// Packet for sending the version of the mod.
/// This packet is sent from the server to the client to inform the client about the version of the mod.
public record VersionPacket(String version) implements CustomPayload {
    public static final Id<VersionPacket> PACKET_ID =
            new Id<>(Identifier.of(PlatformlessInitializer.MOD_ID, "version"));

    public static final PacketCodec<PacketByteBuf, VersionPacket> PACKET_CODEC =
            PacketCodec.ofStatic(
                    (buf, packet) -> PacketCodecs.STRING.encode(buf, packet.version()),
                    (buf) -> {
                        String version = PacketCodecs.STRING.decode(buf);
                        return new VersionPacket(version);
                    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
