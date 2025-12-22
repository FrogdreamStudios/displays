package com.dreamdisplays.commands.subcommands

import com.dreamdisplays.Main.Companion.config
import com.dreamdisplays.utils.Message.sendMessage
import org.bukkit.command.CommandSender

class ReloadCommand : SubCommand {

    override val name = "reload"
    override val permission = config.permissions.reload

    override fun execute(sender: CommandSender, args: Array<String?>) {
        // TODO: should we add a try-catch here?
        config.reload()
        sendMessage(sender, "configReloaded")
    }
}
