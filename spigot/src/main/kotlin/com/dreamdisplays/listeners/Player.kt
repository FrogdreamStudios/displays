package com.dreamdisplays.listeners

import com.dreamdisplays.Main.Companion.config
import com.dreamdisplays.Main.Companion.getInstance
import com.dreamdisplays.managers.PlayerManager
import com.dreamdisplays.managers.PlayerManager.hasBeenNotifiedAboutModRequired
import com.dreamdisplays.managers.PlayerManager.setModRequiredNotified
import com.dreamdisplays.managers.DisplayManager.getDisplays
import com.dreamdisplays.utils.Message.sendColoredMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jspecify.annotations.NullMarked

@NullMarked
class Player : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!config.settings.modDetectionEnabled) return
        if (getDisplays().isEmpty()) return

        getInstance().server.scheduler.runTaskLater(getInstance(), Runnable {
            if (PlayerManager.getVersion(player) == null && !hasBeenNotifiedAboutModRequired(player)) {
                val message = config.messages["modRequired"]
                sendColoredMessage(player, message)
                setModRequiredNotified(player, true)
            }
        }, 600L)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        PlayerManager.removeVersion(event.getPlayer())
    }
}
