package com.inotsleep.dreamdisplays.client_1_21_8.forge;

import com.inotsleep.dreamdisplays.client.agent.JarLoader;
import com.inotsleep.dreamdisplays.client.downloader.Downloader;
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
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod(DreamDisplaysClientCommon.MOD_ID)
public class DreamDisplaysForgeMod implements PacketSender {

    Channel<CustomPacketPayload> channel;

    public DreamDisplaysForgeMod(FMLJavaModLoadingContext context) {
        LoggingManager.setLogger(LogManager.getLogger(DreamDisplaysClientCommon.MOD_ID));
        DreamDisplaysClientCommon.onModInit(this);

        new Thread(() -> {
            try {
                JarLoader.loadLibrariesAtRuntime(new Downloader(Path.of("./libs")).startDownload());
            } catch (IOException e) {
                LoggingManager.error("Unable to load libraries", e);
            }
        }, "Dream Displays downloader thread").start();

        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(this::onCommonSetup);
    }

    public void onCommonSetup(final FMLCommonSetupEvent event) {
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> payloadFlow = ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "main"))
                .clientAcceptedVersions((status, version) -> true)
                .serverAcceptedVersions((status, version) -> true)
                .networkProtocolVersion(1)
                .payloadChannel()
                .play()
                .bidirectional();


        registerChannel(payloadFlow, new DeletePacket(null), wrapHandler(DreamDisplaysClientCommon::onDeletePacket));
        registerChannel(payloadFlow, new DisplayInfoPacket(null, null, 0, 0, 0, 0, 0, null, null, false, null), wrapHandler(DreamDisplaysClientCommon::onDisplayInfoPacket));
        registerChannel(payloadFlow, new PremiumPacket(false), wrapHandler(DreamDisplaysClientCommon::onPremiumPacket));
        registerChannel(payloadFlow, new ReportPacket(null), emptyHandler(ReportPacket.class));
        registerChannel(payloadFlow, new RequestSyncPacket(null), emptyHandler(RequestSyncPacket.class));
        registerChannel(payloadFlow, new SyncPacket(null, false, false, 0, 0), wrapHandler(DreamDisplaysClientCommon::onSyncPacket));
        registerChannel(payloadFlow, new VersionPacket(null), emptyHandler(VersionPacket.class));

        channel = payloadFlow.build();
    }

    private <T extends CustomPacketPayload & PacketCodec<T>> BiConsumer<T, CustomPayloadEvent.Context> emptyHandler(Class<T> claszz) {
        return (payload, context) -> {};
    }

    private <T extends CustomPacketPayload & PacketCodec<T>> BiConsumer<T, CustomPayloadEvent.Context> wrapHandler(Consumer<T> handler) {
        return (t, ctx) -> {
            handler.accept(t);
            ctx.setPacketHandled(true);
        };
    }

    @Override
    public void sendPacket(CustomPacketPayload payload) {
        if (Minecraft.getInstance().getConnection() == null) return;

        channel.send(payload, Minecraft.getInstance().getConnection().getConnection());
    }

    private <T extends CustomPacketPayload & PacketCodec<T>> void registerChannel(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, T packet, BiConsumer<T, CustomPayloadEvent.Context> handler) {
        flow.add(packet.getType(), wrapCodec(packet.getCodec()), handler);
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
