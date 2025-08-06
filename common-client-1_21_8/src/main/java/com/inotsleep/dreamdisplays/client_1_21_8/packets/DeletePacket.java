package com.inotsleep.dreamdisplays.client_1_21_8.packets;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

 /** Packet for deleting a display.
 * This packet is sent from the client to the server to delete a display with the given ID.
 * @param id the ID of the display to delete.
 */
public record DeletePacket(UUID id) implements CustomPacketPayload, PacketCodec<DeletePacket> {

    public static final Type<DeletePacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "delete"));

    public static final StreamCodec<FriendlyByteBuf, DeletePacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeUUID(packet.id()),
                    (buf) -> {
                        UUID id = buf.readUUID();
                        return new DeletePacket(id);
                    });


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

     @Override
     public StreamCodec<FriendlyByteBuf, DeletePacket> getCodec() {
         return PACKET_CODEC;
     }

     @Override
     public CustomPacketPayload.Type<DeletePacket> getType() {
        return PACKET_ID;
     }
 }