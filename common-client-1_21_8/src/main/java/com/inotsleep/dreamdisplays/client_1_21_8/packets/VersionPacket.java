package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Packet for sending the version of the mod.
 * This packet is sent from the server to the client to inform the client about the version of the mod.
 */
public record VersionPacket(String version) implements CustomPacketPayload, PacketCodec<VersionPacket> {
    public static final Type<VersionPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "version"));

    public static final StreamCodec<FriendlyByteBuf, VersionPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeUtf(packet.version()),
                    (buf) -> {
                        String version = buf.readUtf();
                        return new VersionPacket(version);
                    });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    @Override
    public StreamCodec<FriendlyByteBuf, VersionPacket> getCodec() {
        return PACKET_CODEC;
    }

    @Override
    public Class<VersionPacket> getPayloadClass() {
        return VersionPacket.class;
    }
}
