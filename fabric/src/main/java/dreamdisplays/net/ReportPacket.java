package dreamdisplays.net;

import dreamdisplays.PlatformlessInitializer;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Packet for reporting a display.
 * This packet is sent from the client to the server to report about a bad display with the given ID.
 * @param id the ID of the display to report.
 */
public record ReportPacket(UUID id) implements CustomPacketPayload {
    public static final Type<ReportPacket> PACKET_ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlatformlessInitializer.MOD_ID, "report"));

    public static final StreamCodec<FriendlyByteBuf, ReportPacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, packet) -> UUIDUtil.STREAM_CODEC.encode(buf, packet.id()),
                    (buf) -> {
                        UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
                        return new ReportPacket(id);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}