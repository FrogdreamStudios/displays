package com.dreamdisplays.platform.server.mixins

import com.dreamdisplays.platform.server.managers.DisplayManager
import com.dreamdisplays.platform.server.managers.SelectionManager
import com.dreamdisplays.platform.server.utils.RegionUtil
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Suppress("UNUSED", "NonJavaMixin")
@Pseudo
//? if >=1.21.11 {
/*@Mixin(targets = ["net.minecraft.world.level.ServerExplosion"])
open class ExplosionProtectionMixin {
    @Shadow
    @JvmField
    var level: ServerLevel? = null

    // Filters the about-to-be-destroyed positions in place before vanilla acts on them.
    @Inject(method = ["interactWithBlocks"], at = [At("HEAD")], require = 0)
    open fun dd_filterExplodedBlocks(positions: MutableList<BlockPos>, ci: CallbackInfo) {
        val serverLevel = level ?: return
        val worldKey = RegionUtil.getLevelKey(serverLevel)
        positions.removeIf { pos ->
            DisplayManager.isContains(worldKey, pos) != null || SelectionManager.isLocationSelected(pos, worldKey)
        }
    }
}*/
//?} else
@Mixin(targets = ["net.minecraft.world.level.Explosion"])
open class ExplosionProtectionMixin {
    @Shadow
    @JvmField
    var level: Level? = null

    @Shadow
    open fun getToBlow(): MutableList<BlockPos> = throw AssertionError()

    // Filters the vanilla toBlow list in place before it destroys/ignites anything.
    @Inject(method = ["finalizeExplosion"], at = [At("HEAD")], require = 0)
    open fun dd_filterExplodedBlocks(spawnParticles: Boolean, ci: CallbackInfo) {
        val serverLevel = level as? ServerLevel ?: return
        val worldKey = RegionUtil.getLevelKey(serverLevel)
        getToBlow().removeIf { pos ->
            DisplayManager.isContains(worldKey, pos) != null || SelectionManager.isLocationSelected(pos, worldKey)
        }
    }
}
