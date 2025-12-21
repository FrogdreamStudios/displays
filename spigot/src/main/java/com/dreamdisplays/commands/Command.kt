package com.dreamdisplays.commands

import com.dreamdisplays.Main
import com.dreamdisplays.listeners.Selection
import com.dreamdisplays.managers.Display
import com.dreamdisplays.utils.Message
import com.dreamdisplays.utils.Utils
import me.inotsleep.utils.AbstractCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
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
            "stats" -> handleStats(sender)
            "help" -> handleHelp(sender)
            "on" -> handleOn(sender)
            "off" -> handleOff(sender)
            else -> msg(sender, "displayWrongCommand")
        }
    }

    private fun handleCreate(sender: CommandSender) {
        val player = sender as? Player ?: return
        if (!player.hasPermission(Main.config.permissions.create)) {
            msg(player, "displayCommandMissingPermission")
            return
        }

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
        if (!player.hasPermission(Main.config.permissions.delete)) {
            msg(player, "displayCommandMissingPermission")
            return
        }

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
            msg(sender, "displayCommandMissingPermission")
            return
        }

        Main.config.reload()
        msg(sender, "configReloaded")
    }

    private fun handleList(sender: CommandSender) {
        if (!sender.hasPermission(Main.config.permissions.list)) {
            msg(sender, "displayCommandMissingPermission")
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
                val x = d.pos1.blockX
                val y = d.pos1.blockY
                val z = d.pos1.blockZ
                val component = Component.text("${index + 1}. Owner: $owner. Location: ")
                    .append(
                        Component.text("[$x, $y, $z]").color(NamedTextColor.GOLD)
                            .clickEvent(ClickEvent.runCommand("/tp $x $y $z"))
                    )
                    .append(Component.text(", URL: ").color(NamedTextColor.WHITE))
                    .append(Component.text("[YouTube]").color(NamedTextColor.RED).clickEvent(ClickEvent.openUrl(d.url)))
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

    private fun handleStats(sender: CommandSender) {
        if (!sender.hasPermission(Main.config.permissions.stats)) {
            msg(sender, "displayCommandMissingPermission")
            return
        }

        val versions = com.dreamdisplays.managers.Player.getVersions()
        val total = versions.size
        val versionCounts = versions.values.filterNotNull().groupingBy { it.toString() }.eachCount()

        msg(sender, "displayStatsHeader")
        for ((version, count) in versionCounts) {
            val entry = Main.config.getMessageForPlayer(sender as? Player, "displayStatsEntry") as String
            Message.sendColoredMessage(sender, String.format(entry, version, count))
        }
        val totalMsg = Main.config.getMessageForPlayer(sender as? Player, "displayStatsTotal") as String
        Message.sendColoredMessage(sender, String.format(totalMsg, total))
    }

    private fun handleHelp(sender: CommandSender) {
        if (!sender.hasPermission(Main.config.permissions.help)) {
            msg(sender, "displayCommandMissingPermission")
            return
        }

        val header = Main.config.getMessageForPlayer(sender as? Player, "displayHelpHeader")
        Message.sendColoredMessage(sender, "&7D |&f $header")
        if (sender.hasPermission(Main.config.permissions.create)) {
            val create = Main.config.getMessageForPlayer(sender as? Player, "displayHelpCreate")
            Message.sendColoredMessage(sender, "&f $create")
        }
        if (sender.hasPermission(Main.config.permissions.video)) {
            val video = Main.config.getMessageForPlayer(sender as? Player, "displayHelpVideo")
            Message.sendColoredMessage(sender, "&f $video")
        }
        if (sender.hasPermission(Main.config.permissions.delete)) {
            val delete = Main.config.getMessageForPlayer(sender as? Player, "displayHelpDelete")
            Message.sendColoredMessage(sender, "&f $delete")
        }
        if (sender.hasPermission(Main.config.permissions.list)) {
            val list = Main.config.getMessageForPlayer(sender as? Player, "displayHelpList")
            Message.sendColoredMessage(sender, "&f $list")
        }
        if (sender.hasPermission(Main.config.permissions.stats)) {
            val stats = Main.config.getMessageForPlayer(sender as? Player, "displayHelpStats")
            Message.sendColoredMessage(sender, "&f $stats")
        }
        if (sender.hasPermission(Main.config.permissions.reload)) {
            val reload = Main.config.getMessageForPlayer(sender as? Player, "displayHelpReload")
            Message.sendColoredMessage(sender, "&f $reload")
        }
        val on = Main.config.getMessageForPlayer(sender as? Player, "displayHelpOn")
        Message.sendColoredMessage(sender, "&f $on")
        val off = Main.config.getMessageForPlayer(sender as? Player, "displayHelpOff")
        Message.sendColoredMessage(sender, "&f $off")
        val help = Main.config.getMessageForPlayer(sender as? Player, "displayHelpHelp")
        Message.sendColoredMessage(sender, "&f $help")
    }

    private fun msg(sender: CommandSender?, key: String) {
        Message.sendMessage(sender, key)
    }

    private fun sendHelp(sender: CommandSender?) {
        msg(sender, "displayWrongCommand")
    }

    override fun complete(sender: CommandSender, args: Array<String?>): MutableList<String?> {
        if (args.size != 1) return mutableListOf()

        val list = mutableListOf<String?>()
        val perms = Main.config.permissions

        if (sender.hasPermission(perms.create)) list += "create"    // Default: everyone
        if (sender.hasPermission(perms.video)) list += "video"      // Default: everyone
        if (sender.hasPermission(perms.delete)) list += "delete"    // Default: op
        if (sender.hasPermission(perms.list)) list += "list"        // Default: op
        if (sender.hasPermission(perms.reload)) list += "reload"    // Default: op
        list += "on"    // Default: everyone, can't be restricted
        list += "off"   // Default: everyone, can't be restricted
        if (sender.hasPermission(perms.stats)) list += "stats"      // Default: op
        if (sender.hasPermission(perms.help)) list += "help"        // Default: everyone

        return list
    }
}
