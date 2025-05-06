package ru.l0sty.frogdisplays;

import net.minecraft.network.packet.CustomPayload;

public interface Mod {
    void sendPacket(CustomPayload packet);
}
