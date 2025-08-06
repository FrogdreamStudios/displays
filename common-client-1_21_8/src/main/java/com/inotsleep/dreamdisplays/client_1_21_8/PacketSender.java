package com.inotsleep.dreamdisplays.client_1_21_8;

import com.inotsleep.dreamdisplays.client_1_21_8.packets.PacketCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PacketSender {
    <T extends CustomPacketPayload & PacketCodec<T>> void sendPacket(T payload);
}
