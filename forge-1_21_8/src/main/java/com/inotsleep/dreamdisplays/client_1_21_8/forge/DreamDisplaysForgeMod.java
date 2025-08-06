package com.inotsleep.dreamdisplays.client_1_21_8.forge;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import com.inotsleep.dreamdisplays.client_1_21_8.PacketSender;
import com.inotsleep.dreamdisplays.client_1_21_8.packets.*;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import org.apache.logging.log4j.LogManager;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod(DreamDisplaysClientCommon.MOD_ID)
public class DreamDisplaysForgeMod implements PacketSender {

    private static Map<ResourceLocation, SimpleChannel> channels = new HashMap<>();

    public DreamDisplaysForgeMod(FMLJavaModLoadingContext context) {
        LoggingManager.setLogger(LogManager.getLogger(DreamDisplaysClientCommon.MOD_ID));
        DreamDisplaysClientCommon.onModInit(this);

        context.getModBusGroup().register(MethodHandles.lookup(), this);
    }

    @SubscribeEvent
    public void onCommonSetup(final FMLCommonSetupEvent event) {
        registerChannel(new DeletePacket(null), wrapHandler(DreamDisplaysClientCommon::onDeletePacket));
        registerChannel(new DisplayInfoPacket(null, null, null, 0, 0, null, null, false, null), wrapHandler(DreamDisplaysClientCommon::onDisplayInfoPacket));
        registerChannel(new PremiumPacket(false), wrapHandler(DreamDisplaysClientCommon::onPremiumPacket));
        registerChannel(new ReportPacket(null), emptyHandler(ReportPacket.class));
        registerChannel(new RequestSyncPacket(null), emptyHandler(RequestSyncPacket.class));
        registerChannel(new SyncPacket(null, false, false, 0, 0), wrapHandler(DreamDisplaysClientCommon::onSyncPacket));
        registerChannel(new VersionPacket(null), emptyHandler(VersionPacket.class));
    }

    private <T extends CustomPacketPayload & PacketCodec<T>> BiConsumer<T, CustomPayloadEvent.Context> emptyHandler(Class<T> claszz) {
        return (payload, context) -> {};
    }

    private <T extends CustomPacketPayload & PacketCodec<T>> BiConsumer<T, CustomPayloadEvent.Context> wrapHandler(Consumer<T> handler) {
        return (t, ctx) -> {
            ctx.enqueueWork(() -> handler.accept(t));
        };
    }

    @Override
    public void sendPacket(CustomPacketPayload payload) {
        SimpleChannel channel = channels.get(payload.type().id());
        if (channel == null) {
            LoggingManager.error("Could not find channel for " + payload.type().id());
            return;
        }

        if (Minecraft.getInstance().getConnection() == null) return;

        channel.send(payload, Minecraft.getInstance().getConnection().getConnection());
    }

    private <T extends CustomPacketPayload & PacketCodec<T>> void registerChannel(T packet, BiConsumer<T, CustomPayloadEvent.Context> handler) {
        SimpleChannel channel = ChannelBuilder
                .named(packet.type().id())
                .clientAcceptedVersions((status, version) -> true)
                .serverAcceptedVersions((status, version) -> true)
                .networkProtocolVersion(1)
                .simpleChannel()
                    .play()
                        .clientbound()
                            .addMain(packet.getPayloadClass(), wrapCodec(packet.getCodec()), handler)
                        .serverbound()
                            .addMain(packet.getPayloadClass(), wrapCodec(packet.getCodec()), handler)
                .build();

        channels.put(packet.type().id(), channel);
    }

    private <T> StreamCodec<RegistryFriendlyByteBuf, T> wrapCodec(
            StreamCodec<FriendlyByteBuf, T> commonCodec
    ) {
        return StreamCodec.of(
                commonCodec::encode,
                commonCodec::decode
        );
    }
}
