package dreamdisplays;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface Mod {
    void sendPacket(CustomPacketPayload packet);
}