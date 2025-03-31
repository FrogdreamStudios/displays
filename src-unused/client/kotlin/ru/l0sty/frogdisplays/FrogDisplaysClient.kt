package ru.l0sty.frogdisplays

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import org.lwjgl.glfw.GLFW
import ru.l0sty.frogdisplays.cef.CefBrowserCinema
import ru.l0sty.frogdisplays.cef.CefUtil
import kotlin.math.min


object FrogDisplaysClient: ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvents.StartTick { client: MinecraftClient? -> onTick() })
        DisplayBlock.register()
        DisplayBlockEntity.register()
        DisplayBlockEntityRenderer.register()
    }
    val cef: CefBrowserCinema by lazy { CefBrowserCinema(null, "https://www.google.com", true, null) }
    val minecraft: MinecraftClient = MinecraftClient.getInstance()

    val KEY_MAPPING: KeyBinding = KeyBinding(
        "Open Basic Browser", InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_H, "key.categories.misc"
    )
    fun onTick() {
        // Check if our key was pressed
        if (KEY_MAPPING.wasPressed() && minecraft.currentScreen !is BasicBrowser) {

            //Display the web browser UI.
//            minecraft.setScreen(
//                BasicBrowser(
//                    Text.literal("Basic Browser")
//                )
//            )
            val block = getBlockHit()
            val blockEntity = minecraft.world?.setBlockState(block!!, DisplayBlock.defaultState)
            val browser = CefUtil.createBrowser("https://www.google.com")
        }
    }

    fun getBlockHit(): BlockPos? {
        val hit = minecraft.crosshairTarget
        return if (hit != null && hit.type == HitResult.Type.BLOCK) {
            (hit as BlockHitResult).blockPos
        } else {
            null
        }
    }
}