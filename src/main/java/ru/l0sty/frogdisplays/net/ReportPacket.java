package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import ru.l0sty.frogdisplays.PlatformlessInitializer;

import java.util.UUID;

public record ReportPacket(UUID id) implements CustomPayload {
    public static final Id<ReportPacket> PACKET_ID =
            new Id<>(Identifier.of(PlatformlessInitializer.MOD_ID, "report"));

    public static final PacketCodec<PacketByteBuf, ReportPacket> PACKET_CODEC =
            PacketCodec.ofStatic(
                    (buf, packet) -> Uuids.PACKET_CODEC.encode(buf, packet.id()),
                    (buf) -> {
                        UUID id = Uuids.PACKET_CODEC.decode(buf);
                        return new ReportPacket(id);
                    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
