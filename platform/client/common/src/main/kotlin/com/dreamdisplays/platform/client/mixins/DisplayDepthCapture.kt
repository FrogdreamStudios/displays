package com.dreamdisplays.platform.client.mixins

//? if >=1.21.11 {
import com.dreamdisplays.platform.client.render.DisplaySceneSnapshot
import com.mojang.blaze3d.textures.GpuSampler
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Snapshots the world depth and color buffers just before the translucent chunk layer is drawn, so displays can
 * occlude against opaque geometry only and recover the water tint. See [DisplaySceneSnapshot].
 */
@Suppress("NonJavaMixin")
@Mixin(ChunkSectionsToRender::class)
open class DisplayDepthCapture {
    @Inject(
        method = ["renderGroup"],
        at = [At("HEAD")],
        require = 0,
    )
    open fun onRenderGroup(group: ChunkSectionLayerGroup, sampler: GpuSampler, ci: CallbackInfo) {
        if (group.name == "TRANSLUCENT") {
            DisplaySceneSnapshot.captureOpaque()
        }
    }
}
//?} else
/*import net.minecraft.client.renderer.LevelRenderer
import org.spongepowered.asm.mixin.Mixin

@Suppress("NonJavaMixin")
@Mixin(LevelRenderer::class)
open class DisplayDepthCapture*/
