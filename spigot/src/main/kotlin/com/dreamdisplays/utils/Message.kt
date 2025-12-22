package com.dreamdisplays.utils

import com.dreamdisplays.Main
import com.google.gson.Gson
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jspecify.annotations.NullMarked

@NullMarked
object Message {
    fun sendColoredMessage(player: CommandSender?, message: Any?) {
        if (player == null || message == null) return
        if (message is String) {
            val component = LegacyComponentSerializer.legacyAmpersand().deserialize(message)
            val audiences = Main.getInstance().audiences
            if (audiences != null) {
                audiences.sender(player).sendMessage(component)
            } else {
                player.sendMessage(message)
            }
        } else {
            val gson = Gson()
            val jsonStr = gson.toJson(message)
            val component = GsonComponentSerializer.gson().deserialize(jsonStr)
            val audiences = Main.getInstance().audiences
            if (audiences != null) {
                audiences.sender(player).sendMessage(component)
            } else {
                val plain = LegacyComponentSerializer.legacyAmpersand().serialize(component)
                    .replace(Regex("&[0-9a-fk-or]"), "") + " (https://modrinth.com/plugin/dreamdisplays)"
                player.sendMessage(plain)
            }
        }
    }

    fun sendColoredMessages(player: CommandSender?, messages: List<String?>?) {
        if (player == null || messages == null) return
        messages.forEach { message ->
            if (message != null) {
                sendColoredMessage(player, message)
            }
        }
    }

    fun sendComponent(player: CommandSender?, component: Component?) {
        if (player == null || component == null) return
        Main.getInstance().audiences?.sender(player)?.sendMessage(component)
    }

    fun sendMessage(player: CommandSender?, messageKey: String) {
        val message = Main.config.getMessageForPlayer(player as? Player, messageKey)
        sendColoredMessage(player, message)
    }
}
