package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import com.inotsleep.dreamdisplays.client.display.Display;
import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.util.UUID;

/**
 * Packet for displaying information about a display.
 * This packet is sent from the server to the client to provide information about a display.
 */
public record DisplayInfoPacket(UUID id, UUID ownerId, int x, int y, int z, int width, int height, String videoCode, Display.Facing facing, boolean isSync, String lang) implements CustomPacketPayload, PacketCodec<DisplayInfoPacket> {
    public static final CustomPacketPayload.Type<DisplayInfoPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "display_info"));

    public static final StreamCodec<FriendlyByteBuf, DisplayInfoPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        Thread.dumpStack();

                        buf.writeUUID(packet.id());
                        buf.writeUUID(packet.ownerId());

                        buf.writeVarInt(packet.x());
                        buf.writeVarInt(packet.y());
                        buf.writeVarInt(packet.z());

                        buf.writeVarInt(packet.width());
                        buf.writeVarInt(packet.height());

                        buf.writeUtf(packet.videoCode());
                        buf.writeByte(packet.facing.toPacket());

                        buf.writeBoolean(packet.isSync());
                        buf.writeUtf(packet.lang());
                    },
                    (buf) -> {
                        UUID id = buf.readUUID();
                        UUID ownerId = buf.readUUID();

                        int x = buf.readVarInt();
                        int y = buf.readVarInt();
                        int z = buf.readVarInt();

                        int width = buf.readVarInt();
                        int height = buf.readVarInt();

                        String videoCode = buf.readUtf();

                        byte facingByte = buf.readByte();
                        Display.Facing facing = Display.Facing.fromPacket(facingByte);

                        boolean isSync = buf.readBoolean();
                        String lang = buf.readUtf();

                        return new DisplayInfoPacket(id, ownerId, x, y, z, width, height, videoCode, facing, isSync, lang);
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    @Override
    public StreamCodec<FriendlyByteBuf, DisplayInfoPacket> getCodec() {
        return PACKET_CODEC;
    }

    @Override
    public CustomPacketPayload.Type<DisplayInfoPacket> getType() {
        return PACKET_ID;
    }
}