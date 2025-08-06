package com.inotsleep.dreamdisplays.client_1_21_8;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PacketSender {
    void sendPacket(CustomPacketPayload payload);
}
