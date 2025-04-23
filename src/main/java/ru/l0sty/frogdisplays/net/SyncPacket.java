package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import ru.l0sty.frogdisplays.FrogDisplaysMod;

import java.util.UUID;

public record SyncPacket(UUID id, boolean isSync, boolean currentState, long currentTime, long limitTime) implements CustomPayload {
    public static final CustomPayload.Id<SyncPacket> PACKET_ID =
            new CustomPayload.Id<SyncPacket>(Identifier.of(FrogDisplaysMod.MOD_ID, "sync"));

    public static final PacketCodec<PacketByteBuf, SyncPacket> PACKET_CODEC =
            PacketCodec.ofStatic(
                    (buf, packet) -> {
                        Uuids.PACKET_CODEC.encode(buf, packet.id());
                        PacketCodecs.BOOL.encode(buf, packet.isSync());
                        PacketCodecs.BOOL.encode(buf, packet.currentState());
                        PacketCodecs.VAR_LONG.encode(buf, packet.currentTime());
                        PacketCodecs.VAR_LONG.encode(buf, packet.limitTime());
                    },
                    (buf) -> {
                        UUID id = Uuids.PACKET_CODEC.decode(buf);

                        boolean isSync = PacketCodecs.BOOL.decode(buf);
                        boolean currentState = PacketCodecs.BOOL.decode(buf);
                        long currentTime = PacketCodecs.VAR_LONG.decode(buf);
                        long limitTime = PacketCodecs.VAR_LONG.decode(buf);

                        return new SyncPacket(id, isSync, currentState, currentTime, limitTime);
                    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
