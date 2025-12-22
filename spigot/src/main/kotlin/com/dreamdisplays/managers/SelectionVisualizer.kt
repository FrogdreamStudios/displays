package com.dreamdisplays.managers

import com.dreamdisplays.Main
import com.dreamdisplays.utils.Outliner
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

object SelectionVisualizer {
    fun startParticleTask(plugin: Main) {
        if (!Main.config.settings.particlesEnabled) return

        object : BukkitRunnable() {
            override fun run() {
                SelectionManager.selectionPoints.values.forEach { it.drawBox() }
                SelectionManager.selectionPoints.forEach { (playerId, sel) ->
                    Bukkit.getPlayer(playerId)?.let { player ->
                        if (sel.isReady && sel.pos1 != null && sel.pos2 != null)
                            Outliner.showOutline(player, sel.pos1!!, sel.pos2!!)
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, Main.config.settings.particleRenderDelay.toLong())
    }
}
