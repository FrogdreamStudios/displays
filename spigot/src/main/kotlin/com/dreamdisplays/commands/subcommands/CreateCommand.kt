package com.dreamdisplays.commands.subcommands

import com.dreamdisplays.Main.Companion.config
import com.dreamdisplays.listeners.Selection.Companion.isValidDisplay
import com.dreamdisplays.listeners.Selection.Companion.selectionPoints
import com.dreamdisplays.listeners.Selection.Companion.sendErrorMessage
import com.dreamdisplays.managers.DisplayManager
import com.dreamdisplays.managers.DisplayManager.register
import com.dreamdisplays.utils.Message.sendMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreateCommand : SubCommand {

    override val name = "create"
    override val permission = config.permissions.create

    override fun execute(sender: CommandSender, args: Array<String?>) {
        val player = (sender as? Player) ?: return

        val sel = selectionPoints[player.uniqueId]
            ?: return sendMessage(player, "noDisplayTerritories")

        val valid = isValidDisplay(sel)
        if (valid != 6) {
            sendErrorMessage(player, valid)
            return
        }

        if (DisplayManager.isOverlaps(sel)) {
            sendMessage(player, "displayOverlap")
            return
        }

        val displayData = sel.generateDisplayData()
        selectionPoints.remove(player.uniqueId)

        register(displayData)
        sendMessage(player, "successfulCreation")
    }
}
