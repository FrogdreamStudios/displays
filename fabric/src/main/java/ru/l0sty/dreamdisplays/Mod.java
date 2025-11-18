package ru.l0sty.dreamdisplays;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Interface for mods that can send packets.
 */
public interface Mod {
    void sendPacket(CustomPacketPayload packet);
}