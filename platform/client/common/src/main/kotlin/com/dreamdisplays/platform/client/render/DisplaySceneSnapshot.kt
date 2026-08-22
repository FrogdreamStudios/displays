//? if >=1.21.11 {
package com.dreamdisplays.platform.client.render

import com.dreamdisplays.platform.client.Initializer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import java.util.OptionalDouble

/**
 * Copies of the world render target taken at two points in the frame, so displays can be composited as if they
 * were drawn before the translucent geometry while actually being drawn after it.
 *
 * Displays draw once the level pass is over (see [UnshadedDisplayPass]) so no shader pack can touch the picture.
 */
object DisplaySceneSnapshot {
    val DEPTH_ID: Identifier = Identifier.fromNamespaceAndPath(Initializer.MOD_ID, "scene_depth")
    val PRE_COLOR_ID: Identifier = Identifier.fromNamespaceAndPath(Initializer.MOD_ID, "scene_pre_color")
    val POST_COLOR_ID: Identifier = Identifier.fromNamespaceAndPath(Initializer.MOD_ID, "scene_post_color")

    @Volatile
    var ready: Boolean = false
        private set

    @Volatile
    var usable: Boolean = false
        private set

    private var broken = false

    private var shared: GpuSampler? = null

    private fun sampler(): GpuSampler =
        shared ?: RenderSystem.getDevice().createSampler(
            AddressMode.CLAMP_TO_EDGE,
            AddressMode.CLAMP_TO_EDGE,
            FilterMode.NEAREST,
            FilterMode.NEAREST,
            1,
            OptionalDouble.empty(),
        ).also { shared = it }

    private val formatClass: Class<*> by lazy {
        runCatching { Class.forName("com.mojang.blaze3d.textures.TextureFormat") }
            .getOrElse { Class.forName("com.mojang.blaze3d.GpuFormat") }
    }

    private fun format(vararg names: String): Any =
        names.firstNotNullOfOrNull { runCatching { formatClass.getField(it).get(null) }.getOrNull() }
            ?: error("no texture format among ${names.joinToString()}")

    private val createTexture by lazy {
        Class.forName("com.mojang.blaze3d.systems.GpuDevice").getMethod(
            "createTexture",
            String::class.java,
            Int::class.javaPrimitiveType,
            formatClass,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
    }

    private class SnapshotTexture : AbstractTexture() {
        /** Adopts [tex] / [view] / [smp] as this handle's GPU objects. */
        fun install(tex: GpuTexture, view: GpuTextureView, smp: GpuSampler) {
            texture = tex
            textureView = view
            sampler = smp
        }

        override fun close() = Unit
    }

    private class Slot(private val label: String, private val format: Any, private val id: Identifier) {
        private val holder = SnapshotTexture()
        private var texture: GpuTexture? = null
        private var width = 0
        private var height = 0
        private var registered = false

        // Returns the snapshot texture, (re)creating it when the window size changed
        fun ensure(w: Int, h: Int): GpuTexture {
            val existing = texture
            if (existing != null && width == w && height == h) return existing

            release()
            val device = RenderSystem.getDevice()
            val created = createTexture.invoke(
                device,
                label,
                GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                format,
                w,
                h,
                1,
                1,
            ) as GpuTexture
            holder.install(created, device.createTextureView(created), sampler())
            if (!registered) {
                Minecraft.getInstance().textureManager.register(id, holder)
                registered = true
            }
            texture = created
            width = w
            height = h
            return created
        }

        // Frees this slot's GPU texture
        fun release() {
            runCatching { texture?.close() }
            texture = null
            width = 0
            height = 0
        }
    }

    private val depth by lazy { Slot("dream-displays-scene-depth", format("DEPTH32", "D32_FLOAT"), DEPTH_ID) }
    private val preColor by lazy {
        Slot("dream-displays-scene-pre-color", format("RGBA8", "RGBA8_UNORM"), PRE_COLOR_ID)
    }
    private val postColor by lazy {
        Slot("dream-displays-scene-post-color", format("RGBA8", "RGBA8_UNORM"), POST_COLOR_ID)
    }

    fun captureOpaque() {
        if (broken) return
        val target = OutputTarget.MAIN_TARGET.renderTarget ?: return
        if (!target.useDepth) return
        val sourceDepth = target.depthTexture ?: return
        val sourceColor = target.colorTexture ?: return
        val w = target.width
        val h = target.height
        if (w <= 0 || h <= 0) return

        runCatching {
            val encoder = RenderSystem.getDevice().createCommandEncoder()
            encoder.copyTextureToTexture(sourceDepth, depth.ensure(w, h), 0, 0, 0, 0, 0, w, h)
            encoder.copyTextureToTexture(sourceColor, preColor.ensure(w, h), 0, 0, 0, 0, 0, w, h)
            ready = true
            usable = true
        }.onFailure { fail() }
    }

    fun captureBlended() {
        if (broken || !ready) return
        val target = OutputTarget.MAIN_TARGET.renderTarget ?: return
        val sourceColor = target.colorTexture ?: return
        val w = target.width
        val h = target.height
        if (w <= 0 || h <= 0) return

        runCatching {
            RenderSystem.getDevice().createCommandEncoder()
                .copyTextureToTexture(sourceColor, postColor.ensure(w, h), 0, 0, 0, 0, 0, w, h)
        }.onFailure { fail() }
    }

    fun invalidate() {
        ready = false
    }

    private fun fail() {
        broken = true
        ready = false
        usable = false
        depth.release()
        preColor.release()
        postColor.release()
    }
}
//?}
