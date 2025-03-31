package ru.l0sty.frogdisplays.buffer;

import net.minecraft.network.PacketByteBuf;

public interface PacketByteBufSerializable<T extends PacketByteBufSerializable> {

    T fromBytes(PacketByteBuf buf);

    void toBytes(PacketByteBuf buf);

}
