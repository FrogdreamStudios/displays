package ru.l0sty.dreamdisplays.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.l0sty.dreamdisplays.PlatformlessInitializer;

/// Sent from the server to the client to indicate whether the user has a premium.
/// This packet is used to enable or disable premium features (in the client)
/// @param premium true if the user has a premium, false otherwise.
public record PremiumPacket(boolean premium) implements CustomPayload {
    public static final Id<PremiumPacket> PACKET_ID =
            new Id<>(Identifier.of(PlatformlessInitializer.MOD_ID, "premium"));

    public static final PacketCodec<PacketByteBuf, PremiumPacket> PACKET_CODEC =
            PacketCodec.ofStatic(
                    (buf, packet) -> PacketCodecs.BOOLEAN.encode(buf, packet.premium()),
                    (buf) -> {
                        boolean premium = PacketCodecs.BOOLEAN.decode(buf);
                        return new PremiumPacket(premium);
                    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
