package com.dreamdisplays;

import com.dreamdisplays.net.Packets.*;
import com.dreamdisplays.render.ScreenRenderer;
import com.dreamdisplays.screen.Manager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DreamDisplaysMod implements ClientModInitializer, Mod {

    @Override
    public void onInitializeClient() {
        Initializer.onModInit(this);

        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, dedicated) ->
                dispatcher.register(
                    LiteralArgumentBuilder.<FabricClientCommandSource>literal(
                        "display"
                    ).then(
                            LiteralArgumentBuilder.<
                                    FabricClientCommandSource
                                >literal("off").executes(context -> {
                                Initializer.displaysEnabled = false;
                                if (Minecraft.getInstance().player != null) {
                                    Minecraft.getInstance().player.displayClientMessage(Component.literal("§7D | §f").append(Component.translatable("dreamdisplays.display.disabled")), false);
                                }
                                return 1;
                            })
                        ).then(
                            LiteralArgumentBuilder.<
                                    FabricClientCommandSource
                                >literal("on").executes(context -> {
                                Initializer.displaysEnabled = true;
                                if (Minecraft.getInstance().player != null) {
                                    Minecraft.getInstance().player.displayClientMessage(Component.literal("§7D | §f").append(Component.translatable("dreamdisplays.display.enabled")), false);
                                }
                                return 1;
                            })
                        )
                )
        );

        PayloadTypeRegistry.playS2C().register(
            Info.PACKET_ID,
            Info.PACKET_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
            Sync.PACKET_ID,
            Sync.PACKET_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
            Premium.PACKET_ID,
            Premium.PACKET_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
            Delete.PACKET_ID,
            Delete.PACKET_CODEC
        );

        PayloadTypeRegistry.playC2S().register(
            Sync.PACKET_ID,
            Sync.PACKET_CODEC
        );

        PayloadTypeRegistry.playC2S().register(
            RequestSync.PACKET_ID,
            RequestSync.PACKET_CODEC
        );

        PayloadTypeRegistry.playC2S().register(
            Delete.PACKET_ID,
            Delete.PACKET_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
            Report.PACKET_ID,
            Report.PACKET_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
            Version.PACKET_ID,
            Version.PACKET_CODEC
        );

        ClientPlayNetworking.registerGlobalReceiver(
            Info.PACKET_ID,
            (payload, unused) -> Initializer.onDisplayInfoPacket(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
            Premium.PACKET_ID,
            (payload, unused) -> Initializer.onPremiumPacket(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
            Delete.PACKET_ID,
            (deletePacket, unused) -> Initializer.onDeletePacket(deletePacket)
        );

        ClientPlayNetworking.registerGlobalReceiver(
            Sync.PACKET_ID,
            (payload, unused) -> Initializer.onSyncPacket(payload)
        );

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }
            PoseStack matrices = context.matrices();
            Camera camera = context.gameRenderer().getMainCamera();
            ScreenRenderer.render(matrices, camera);
        });

        ClientTickEvents.END_CLIENT_TICK.register(Initializer::onEndTick);

        // Load displays when joining a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.level != null && client.player != null) {
                // Use server address as server ID for local singleplayer worlds
                // TODO: add support for singleplayer in the future.
                // For now, we just use "singleplayer" as the ID.
                String serverId = client.isLocalServer()
                    ? "singleplayer"
                    : (client.getCurrentServer() != null
                          ? client.getCurrentServer().ip
                          : "unknown");
                Manager.loadScreensForServer(serverId);
            }
        });

        // Save displays when disconnecting from a world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Manager.saveAllScreens();
            Manager.unloadAll();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraftClient ->
            Initializer.onStop()
        );
    }

    @Override
    public void sendPacket(CustomPacketPayload packet) {
        ClientPlayNetworking.send(packet);
    }
}
