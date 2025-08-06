package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Packet for synchronizing display data.
 * Read more about the synchronization feature RequestSyncPacket.
 */
public record SyncPacket(UUID id, boolean isSync, boolean currentState, long currentTime, long limitTime) implements CustomPacketPayload, PacketCodec<SyncPacket> {
    public static final Type<SyncPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeUUID(packet.id);
                        buf.writeBoolean(packet.isSync);
                        buf.writeBoolean(packet.currentState);
                        buf.writeLong(packet.currentTime);
                        buf.writeLong(packet.limitTime);
                    },
                    (buf) -> {

                        UUID id = buf.readUUID();

                        boolean isSync = buf.readBoolean();
                        boolean currentState = buf.readBoolean();
                        long currentTime = buf.readLong();
                        long limitTime = buf.readLong();

                        return new SyncPacket(id, isSync, currentState, currentTime, limitTime);
                    });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    @Override
    public StreamCodec<FriendlyByteBuf, SyncPacket> getCodec() {
        return PACKET_CODEC;
    }

    @Override
    public Class<SyncPacket> getPayloadClass() {
        return SyncPacket.class;
    }
}
