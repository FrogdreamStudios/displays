package com.dreamdisplays.platform.client

//? if >=1.21.11 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback*/
//? if >=26 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
//?} else
/*
//? if ==1.21.11 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
//?}
//? if <1.21.11 {
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
//?}
*/
//? if >=1.21.11 {
import net.minecraft.resources.Identifier
//?} else
/*import net.minecraft.resources.ResourceLocation as Identifier*/
import com.dreamdisplays.api.platform.service.keys.PlatformServices
import com.dreamdisplays.platform.client.core.DreamServices
import com.dreamdisplays.platform.client.displays.DisplayRegistry
import com.dreamdisplays.platform.client.net.Packets
import com.dreamdisplays.platform.client.net.V2Payload
import com.dreamdisplays.platform.client.platform.FabricPlatformIntegrationProvider
import com.dreamdisplays.platform.client.render.ScreenRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class Client : ClientModInitializer, Mod {
    /** Called on client initialization. */
    override fun onInitializeClient() {
        // The Platform must be in the registry before onModInit so ClientStartupManager
        // can host the ClientApplication on top of it during bootstrap.
        DreamServices.registry.register(PlatformServices.PLATFORM, FabricPlatformIntegrationProvider.create())
        Initializer.onModInit(this)

        // Note: PayloadTypeRegistry registrations are done in platform/server/ (it's a main entrypoint)
        // which runs on both integrated and dedicated servers, before the client entrypoint.

        // Protocol v2: every packet arrives as one opaque envelope payload
        ClientPlayNetworking.registerGlobalReceiver(V2Payload.TYPE) { payload, _ ->
            Initializer.onV2Packet(payload.bytes)
        }

        // Frozen v1 receivers for pre-v2 servers; payloads are lifted into v2 packets
        listOf(
            Packets.Info.PACKET_ID, Packets.Premium.PACKET_ID, Packets.IsAdmin.PACKET_ID,
            Packets.Delete.PACKET_ID, Packets.DisplayEnabled.PACKET_ID, Packets.Sync.PACKET_ID,
            Packets.ReportEnabled.PACKET_ID, Packets.ClearCache.PACKET_ID,
        ).forEach { type ->
            ClientPlayNetworking.registerGlobalReceiver(type) { payload, _ ->
                Initializer.onLegacyPacket(payload)
            }
        }

        //? if >=26 {
        // Last opaque stage: the terrain still owns the depth buffer, and the translucent targets have not
        // copied it yet, so water and glass depth-test and blend against the display instead of hiding it.
        LevelRenderEvents.AFTER_SOLID_FEATURES.register { context ->
            val mc = Minecraft.getInstance()
            if (mc.level != null && mc.player != null) {
                ScreenRenderer.render(context.poseStack(), mainCamera(mc))
            }
        }

        LevelRenderEvents.END_MAIN.register { context ->
            val mc = Minecraft.getInstance()
            if (mc.level != null && mc.player != null) {
                // Render popout windows after all Minecraft / mod rendering is submitted,
                // so any GL-context switch (macOS GLFW backend) does not disturb in-flight commands.
                DisplayRegistry.getScreens().forEach { it.renderPopout() }
            }
        }

        //?} else
        /*WorldRenderEvents.AFTER_ENTITIES.register { context ->
            val mc = Minecraft.getInstance()
            if (mc.level != null && mc.player != null) {
                val stack = worldPoseStack(context)
                val camera = mainCamera(mc)
                ScreenRenderer.render(stack, camera)
                DisplayRegistry.getScreens().forEach { it.renderPopout() }
            }
        }*/

        //? if >=1.21.11 {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(Initializer.MOD_ID, "pip_overlay")
        ) { graphics, deltaTracker ->
            Initializer.onRenderHud(
                Minecraft.getInstance(), graphics,
                deltaTracker.getGameTimeDeltaPartialTick(false)
            )
        }
        //?} else
        /*HudRenderCallback.EVENT.register { graphics, deltaTracker ->
            Initializer.onRenderHud(
                Minecraft.getInstance(), graphics,
                deltaTracker.getGameTimeDeltaPartialTick(false)
            )
        }*/

        ClientTickEvents.END_CLIENT_TICK.register { Initializer.onEndTick(it) }

        ClientPlayConnectionEvents.JOIN.register { _, _, client ->
            if (client.level != null && client.player != null) {
                val serverId = if (client.isLocalServer) "singleplayer"
                else client.currentServer?.ip ?: "unknown"
                Initializer.onServerJoined(serverId)
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            Initializer.onServerLeft()
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register { Initializer.onStop() }
    }

    /** Packet sender. */
    override fun sendPacket(packet: CustomPacketPayload) {
        ClientPlayNetworking.send(packet)
    }

    /** Main camera accessor. */
    private fun mainCamera(mc: Minecraft): Camera {
        //? if >=26.2 {
        return mc.gameRenderer.mainCamera()
        //?} else
        /*return mc.gameRenderer.getMainCamera()*/
    }

    /** World pose stack accessor. */
    private fun worldPoseStack(context: Any): PoseStack =
        runCatching { context.javaClass.getMethod("matrixStack") }
            .getOrElse { context.javaClass.getMethod("matrices") }
            .invoke(context) as PoseStack
}
