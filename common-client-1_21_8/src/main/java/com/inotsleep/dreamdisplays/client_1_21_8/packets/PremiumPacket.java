package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


/**
 * Sent from the server to the client to indicate whether the user has a premium.
 * This packet is used to enable or disable premium features (in the client)
 * @param premium true if the user has a premium, false otherwise.
 */
public record PremiumPacket(boolean premium) implements CustomPacketPayload {
    public static final Type<PremiumPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "premium"));

    public static final StreamCodec<FriendlyByteBuf, PremiumPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeBoolean(packet.premium()),
                    (buf) -> {
                        boolean premium = buf.readBoolean();
                        return new PremiumPacket(premium);
                    });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}