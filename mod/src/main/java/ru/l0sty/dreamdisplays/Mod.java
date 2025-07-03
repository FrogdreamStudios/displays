package ru.l0sty.dreamdisplays;

import net.minecraft.network.packet.CustomPayload;

///  Interface for mods that can send packets.
public interface Mod {
    void sendPacket(CustomPayload packet);
}
