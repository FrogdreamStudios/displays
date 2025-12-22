package com.dreamdisplays.commands.subcommands

import com.dreamdisplays.Main.Companion.config
import com.dreamdisplays.managers.PlayerManager.getVersions
import com.dreamdisplays.utils.Message.sendColoredMessage
import com.dreamdisplays.utils.Message.sendMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StatsCommand : SubCommand {

    override val name = "stats"
    override val permission = config.permissions.stats

    override fun execute(sender: CommandSender, args: Array<String?>) {
        val versions = getVersions()
        val counts = versions.values.filterNotNull().groupingBy { it }.eachCount()

        sendMessage(sender, "displayStatsHeader")

        for ((version, count) in counts) {
            val msg = config.getMessageForPlayer(sender as? Player, "displayStatsEntry")
            sendColoredMessage(sender, String.format(msg as String, version, count))
        }
    }
}
