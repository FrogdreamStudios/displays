package com.dreamdisplays.platform.client.render

import com.dreamdisplays.platform.client.managers.ClientStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import org.joml.Matrix4f

/**
 * Draws every display after the level pass has completely finished, so no shader pack can touch the picture.
 */
object UnshadedDisplayPass {
    @Volatile
    private var tailHookAlive = false

    private var pendingCamera: Camera? = null

    private val pendingView = Matrix4f()

    private var pending = false

    private val enabled: Boolean; get() = runCatching { ClientStateManager.config.unshadedDisplays }.getOrDefault(true)

    val active: Boolean get() = tailHookAlive && enabled

    fun capture(stack: PoseStack, camera: Camera): Boolean {
        if (!active) {
            pending = false
            return false
        }
        pendingCamera = camera
        pendingView.set(RenderSystem.getModelViewStack()).mul(stack.last().pose())
        pending = true
        return true
    }

    fun drawPending() {
        tailHookAlive = true
        if (!pending) return
        pending = false
        val camera = pendingCamera ?: return
        if (Minecraft.getInstance().level == null) return

        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        modelView.set(pendingView)
        //? if >=1.21.11 {
        //?} else
        /*RenderSystem.applyModelViewMatrix()*/
        try {
            ScreenRenderer.render(PoseStack(), camera)
        } finally {
            modelView.popMatrix()
            //? if >=1.21.11 {
            //?} else
            /*RenderSystem.applyModelViewMatrix()*/
        }
    }
}
