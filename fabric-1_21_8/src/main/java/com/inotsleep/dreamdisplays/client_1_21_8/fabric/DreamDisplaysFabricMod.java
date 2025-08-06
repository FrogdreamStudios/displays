package com.inotsleep.dreamdisplays.client_1_21_8.fabric;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import com.inotsleep.dreamdisplays.client_1_21_8.PacketSender;
import com.inotsleep.dreamdisplays.client_1_21_8.packets.*;
import me.inotsleep.utils.logging.LoggingManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.slf4j.LoggerFactory;

public class DreamDisplaysFabricMod implements ClientModInitializer, PacketSender {
    @Override
    public void onInitializeClient() {
        LoggingManager.setLogger(LoggerFactory.getLogger(DreamDisplaysClientCommon.MOD_ID));

        PayloadTypeRegistry.playS2C().register(DisplayInfoPacket.PACKET_ID, DisplayInfoPacket.PACKET_CODEC);

        PayloadTypeRegistry.playS2C().register(SyncPacket.PACKET_ID, SyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(SyncPacket.PACKET_ID, SyncPacket.PACKET_CODEC);

        PayloadTypeRegistry.playC2S().register(RequestSyncPacket.PACKET_ID, RequestSyncPacket.PACKET_CODEC);

        PayloadTypeRegistry.playC2S().register(DeletePacket.PACKET_ID, DeletePacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ReportPacket.PACKET_ID, ReportPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(VersionPacket.PACKET_ID, VersionPacket.PACKET_CODEC);

        PayloadTypeRegistry.playS2C().register(DeletePacket.PACKET_ID, DeletePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(PremiumPacket.PACKET_ID, PremiumPacket.PACKET_CODEC);


        ClientPlayNetworking.registerGlobalReceiver(DisplayInfoPacket.PACKET_ID, (payload, context) -> DreamDisplaysClientCommon.onDisplayInfoPacket(payload));
        ClientPlayNetworking.registerGlobalReceiver(PremiumPacket.PACKET_ID, (payload, unused) -> DreamDisplaysClientCommon.onPremiumPacket(payload));
        ClientPlayNetworking.registerGlobalReceiver(DeletePacket.PACKET_ID, (deletePacket, unused) -> DreamDisplaysClientCommon.onDeletePacket(deletePacket));

        ClientPlayNetworking.registerGlobalReceiver(SyncPacket.PACKET_ID, (payload, unused) -> DreamDisplaysClientCommon.onSyncPacket(payload));

        DreamDisplaysClientCommon.onModInit(this);
    }

    @Override
    public void sendPacket(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
