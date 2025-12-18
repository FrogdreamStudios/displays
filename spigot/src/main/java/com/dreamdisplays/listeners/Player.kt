package com.dreamdisplays.listeners

import com.dreamdisplays.Main
import com.dreamdisplays.managers.Player
import com.dreamdisplays.managers.Player.hasBeenNotifiedAboutModRequired
import com.dreamdisplays.managers.Player.setModRequiredNotified
import com.dreamdisplays.utils.Message
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
        if (!Main.config.settings.modDetectionEnabled) return

        Main.getInstance().server.scheduler.runTaskLater(Main.getInstance(), Runnable {
            if (Player.getVersion(player) == null && !hasBeenNotifiedAboutModRequired(player)) {
                val message = Main.config.messages["modRequired"] as? String ?: "&7D |&f Dream Displays mod is required to see displays on the server. Please install it for a good experience!"
                Message.sendColoredMessage(player, message)
                setModRequiredNotified(player, true)
            }
        }, 200L)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        Player.removeVersion(event.getPlayer())
    }
}
