package dreamdisplays.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dreamdisplays.PlatformlessInitializer;

/**
 * Sent from the server to the client to indicate whether the user has a premium.
 * This packet is used to enable or disable premium features (in the client)
 * @param premium true if the user has a premium, false otherwise.
 */
public record PremiumPacket(boolean premium) implements CustomPacketPayload {
    public static final Type<PremiumPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlatformlessInitializer.MOD_ID, "premium"));

    public static final StreamCodec<FriendlyByteBuf, PremiumPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> ByteBufCodecs.BOOL.encode(buf, packet.premium()),
                    (buf) -> {
                        boolean premium = ByteBufCodecs.BOOL.decode(buf);
                        return new PremiumPacket(premium);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}