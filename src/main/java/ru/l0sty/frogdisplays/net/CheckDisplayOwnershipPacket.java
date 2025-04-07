package ru.l0sty.frogdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record CheckDisplayOwnershipPacket(UUID id, boolean isOwner) implements CustomPayload {
    public static final CustomPayload.Id<CheckDisplayOwnershipPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("frogdisplays", "check_ownership"));

    public static final PacketCodec<PacketByteBuf, CheckDisplayOwnershipPacket> PACKET_CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, CheckDisplayOwnershipPacket::id,
                    PacketCodecs.BOOL, CheckDisplayOwnershipPacket::isOwner,
                    CheckDisplayOwnershipPacket::new
            ).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
