package com.dreamdisplays.commands

import com.dreamdisplays.Main
import com.dreamdisplays.listeners.Selection
import com.dreamdisplays.managers.Display
import com.dreamdisplays.utils.Message
import com.dreamdisplays.utils.Utils
import me.inotsleep.utils.AbstractCommand
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jspecify.annotations.NullMarked

@NullMarked
class Command : AbstractCommand(Main.getInstance().name, "display") {

    override fun toExecute(sender: CommandSender, s: String?, args: Array<String?>) {
        when (args.size) {
            0 -> sendHelp(sender)
            1 -> handleSingle(sender, args[0])
            2, 3 -> handleVideo(sender, args)
        }
    }

    private fun handleVideo(sender: CommandSender, args: Array<String?>) {
        val player = sender as? Player ?: return
        if (!player.hasPermission(Main.config.permissions.video)) return
        if (args[0] != "video") return

        val block = player.getTargetBlock(null, 32)
        if (block.type != Main.config.settings.baseMaterial) {
            msg(player, "noDisplay")
            return
        }

        val data = Display.isContains(block.location)
        if (data == null || data.ownerId != player.uniqueId) {
            msg(player, "noDisplay")
            return
        }

        val code = Utils.extractVideo(args[1] ?: "")
        if (code == null) {
            msg(player, "invalidURL")
            return
        }

        data.url = "https://youtube.com/watch?v=$code"
        data.lang = args.getOrNull(2) ?: ""
        data.isSync = false
        data.sendUpdatePacket(data.receivers)

        msg(player, "settedURL")
    }

    private fun handleSingle(sender: CommandSender, arg: String?) {
        when (arg) {
            "create" -> handleCreate(sender)
            "delete" -> handleDelete(sender)
            "reload" -> handleReload(sender)
            "list" -> handleList(sender)
            "on" -> handleOn(sender)
            "off" -> handleOff(sender)
        }
    }

    private fun handleCreate(sender: CommandSender) {
        val player = sender as? Player ?: return
        if (!player.hasPermission(Main.config.permissions.create)) return

        val sel = Selection.selectionPoints[player.uniqueId]
            ?: return msg(player, "noDisplayTerritories")

        val valid = Selection.isValidDisplay(sel)
        if (valid != 6) {
            Selection.sendErrorMessage(player, valid)
            return
        }

        if (Display.isOverlaps(sel)) {
            msg(player, "displayOverlap")
            return
        }

        val displayData = sel.generateDisplayData()
        Selection.selectionPoints.remove(player.uniqueId)

        Display.register(displayData)
        msg(player, "successfulCreation")
    }

    private fun handleDelete(sender: CommandSender) {
        val player = sender as? Player ?: return
        if (!player.hasPermission(Main.config.permissions.delete)) return

        val block = player.getTargetBlock(null, 32)
        if (block.type != Main.config.settings.baseMaterial) {
            msg(player, "noDisplay")
            return
        }

        val data = Display.isContains(block.location)
            ?: return msg(player, "noDisplay")

        Display.delete(data)
        msg(player, "displayDeleted")
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission(Main.config.permissions.reload)) {
            sendHelp(sender)
            return
        }

        Main.config.reload()
        msg(sender, "configReloaded")
    }

    private fun handleList(sender: CommandSender) {
        if (!sender.isOp) {
            sendHelp(sender)
            return
        }

        val displays = Display.getDisplays()
        if (displays.isEmpty()) {
            msg(sender, "noDisplaysFound")
            return
        }

        msg(sender, "displayListHeader")

        val audiences = Main.getInstance().audiences
        if (audiences != null) {
            displays.forEachIndexed { index, d ->
                val owner = Bukkit.getOfflinePlayer(d.ownerId).name ?: "Unknown"
                val locationText = "[${d.pos1.blockX}, ${d.pos1.blockY}, ${d.pos1.blockZ}]"
                val component = Component.text("${index + 1}. Owner: $owner, Location: ")
                    .append(Component.text(locationText))
                    .append(Component.text(", URL: ${d.url}"))
                Message.sendComponent(sender, component)
            }
        } else {
            val entry = Main.config.messages["displayListEntry"] as String?
            displays.forEachIndexed { index, d ->
                val owner = Bukkit.getOfflinePlayer(d.ownerId).name ?: "Unknown"
                val formatted = me.inotsleep.utils.MessageUtil.parsePlaceholders(
                    entry,
                    (index + 1).toString(),
                    owner,
                    d.pos1.blockX.toString(),
                    d.pos1.blockY.toString(),
                    d.pos1.blockZ.toString(),
                    d.url
                )
                Message.sendColoredMessage(sender, formatted)
            }
        }
    }

    private fun handleOn(sender: CommandSender) {
        val player = sender as? Player ?: return
        if (com.dreamdisplays.managers.Player.isDisplaysEnabled(player)) {
            msg(player, "display.already-enabled")
            return
        }
        com.dreamdisplays.managers.Player.setDisplaysEnabled(player, true)
        com.dreamdisplays.utils.net.Utils.sendDisplayEnabledPacket(player, true)
        msg(player, "display.enabled")
    }

    private fun handleOff(sender: CommandSender) {
        val player = sender as? Player ?: return
        if (!com.dreamdisplays.managers.Player.isDisplaysEnabled(player)) {
            msg(player, "display.already-disabled")
            return
        }
        com.dreamdisplays.managers.Player.setDisplaysEnabled(player, false)
        com.dreamdisplays.utils.net.Utils.sendDisplayEnabledPacket(player, false)
        msg(player, "display.disabled")
    }

    private fun msg(sender: CommandSender?, key: String) {
        Message.sendMessage(sender, key)
    }

    private fun sendHelp(sender: CommandSender?) {
        Message.sendColoredMessages(sender, Message.getMessages("displayCommandHelp"))
    }

    override fun complete(sender: CommandSender, args: Array<String?>): MutableList<String?> {
        if (args.size != 1) return mutableListOf()

        val list = mutableListOf<String?>()
        val perms = Main.config.permissions

        if (sender.hasPermission(perms.create)) list += "create"
        if (sender.hasPermission(perms.video)) list += "video"
        if (sender.hasPermission(perms.delete)) list += "delete"
        if (sender.hasPermission(perms.list)) list += "list"
        if (sender.hasPermission(perms.reload)) list += "reload"
        list += "on"
        list += "off"

        return list
    }
}
