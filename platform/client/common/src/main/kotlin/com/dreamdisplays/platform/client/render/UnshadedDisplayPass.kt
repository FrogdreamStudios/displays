package com.dreamdisplays.platform.client.render

//? if >=1.21.11 {
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.buffers.GpuBufferSlice
//?}
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

    //? if >=1.21.11 {
    private var pendingProjection: GpuBufferSlice? = null

    private var pendingProjectionType: ProjectionType? = null

    private var pendingFog: GpuBufferSlice? = null
    //?}

    private val enabled: Boolean; get() = runCatching { ClientStateManager.config.unshadedDisplays }.getOrDefault(true)

    val active: Boolean get() = tailHookAlive && enabled

    /**
     * Captures the world transform of the current level pass and defers the draw to [drawPending].
     * Returns false when the late pass is unavailable, in which case the caller must draw the displays itself.
     */
    fun capture(stack: PoseStack, camera: Camera): Boolean {
        if (!active) {
            pending = false
            return false
        }
        pendingCamera = camera
        pendingView.set(RenderSystem.getModelViewStack()).mul(stack.last().pose())
        //? if >=1.21.11 {
        pendingProjection = RenderSystem.getProjectionMatrixBuffer()
        pendingProjectionType = RenderSystem.getProjectionType()
        pendingFog = RenderSystem.getShaderFog()
        //?}
        pending = true
        return true
    }

    /**
     * Draws the frame captured by [capture], with the world's view / projection / fog state restored around it.
     *
     * Called from the `GameRenderer` tail injection once the level pass (shader chain included) is complete.
     */
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
        val projection = pendingProjection
        val projectionType = pendingProjectionType
        val fog = pendingFog
        val savedProjection: GpuBufferSlice? = RenderSystem.getProjectionMatrixBuffer()
        val savedProjectionType: ProjectionType? = RenderSystem.getProjectionType()
        val savedFog: GpuBufferSlice? = RenderSystem.getShaderFog()
        if (projection != null && projectionType != null) RenderSystem.setProjectionMatrix(projection, projectionType)
        if (fog != null) RenderSystem.setShaderFog(fog)
        //?} else
        /*RenderSystem.applyModelViewMatrix()*/
        try {
            ScreenRenderer.render(PoseStack(), camera)
        } finally {
            //? if >=1.21.11 {
            if (fog != null && savedFog != null) RenderSystem.setShaderFog(savedFog)
            if (projection != null && savedProjection != null && savedProjectionType != null) {
                RenderSystem.setProjectionMatrix(savedProjection, savedProjectionType)
            }
            //?}
            modelView.popMatrix()
            //? if >=1.21.11 {
            //?} else
            /*RenderSystem.applyModelViewMatrix()*/
        }
    }
}
