package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PacketCodec<T extends CustomPacketPayload> {
    StreamCodec<FriendlyByteBuf, T > getCodec();
    Class<T> getPayloadClass();
}
