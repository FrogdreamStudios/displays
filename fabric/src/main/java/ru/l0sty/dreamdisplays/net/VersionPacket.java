package ru.l0sty.dreamdisplays.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.l0sty.dreamdisplays.PlatformlessInitializer;

/**
 * Packet for sending the version of the mod.
 * This packet is sent from the server to the client to inform the client about the version of the mod.
 */
public record VersionPacket(String version) implements CustomPacketPayload {
    public static final Type<VersionPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlatformlessInitializer.MOD_ID, "version"));

    public static final StreamCodec<FriendlyByteBuf, VersionPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> ByteBufCodecs.STRING_UTF8.encode(buf, packet.version()),
                    (buf) -> {
                        String version = ByteBufCodecs.STRING_UTF8.decode(buf);
                        return new VersionPacket(version);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}