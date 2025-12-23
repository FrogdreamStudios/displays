package com.dreamdisplays.utils

import com.dreamdisplays.Main.Companion.config
import com.dreamdisplays.Main.Companion.getInstance
import com.google.gson.Gson
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jspecify.annotations.NullMarked

/**
 * Manager for sending messages to players/command senders.
 * Supports different message formats: Component, legacy strings, and JSON.
 * Works with Adventure API if available, otherwise falls back to text messages.
 */
@NullMarked
object Message {
    private val gson by lazy { Gson() }
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
    private val gsonSerializer = GsonComponentSerializer.gson()

    private const val FALLBACK_SUFFIX = " (https://modrinth.com/plugin/dreamdisplays)"

    fun sendMessage(sender: CommandSender?, messageKey: String) {
        val message = config.getMessageForPlayer(sender as? Player, messageKey)
        sendColoredMessage(sender, message)
    }

    fun sendColoredMessage(sender: CommandSender?, message: Any?) {
        if (sender == null || message == null) return

        when (message) {
            is Component -> sendComponent(sender, message)
            is String -> sendLegacyMessage(sender, message)
            else -> sendJsonMessage(sender, message)
        }
    }

    fun sendComponent(sender: CommandSender?, component: Component?) {
        if (sender == null || component == null) return

        getInstance().audiences?.sender(sender)?.sendMessage(component)
    }

    private fun sendLegacyMessage(sender: CommandSender, message: String) {
        val component = legacySerializer.deserialize(message)
        val audiences = getInstance().audiences

        if (audiences != null) {
            audiences.sender(sender).sendMessage(component)
        } else {
            // Fallback for servers without Adventure API
            sender.sendMessage(message)
        }
    }

    private fun sendJsonMessage(sender: CommandSender, message: Any) {
        val jsonString = gson.toJson(message)
        val component = gsonSerializer.deserialize(jsonString)
        val audiences = getInstance().audiences

        if (audiences != null) {
            audiences.sender(sender).sendMessage(component)
        } else {
            // Fallback for servers without adequate format support (like in Bukkit)
            val plainText = stripFormattingCodes(legacySerializer.serialize(component)) + FALLBACK_SUFFIX
            sender.sendMessage(plainText)
        }
    }

    private fun stripFormattingCodes(text: String): String {
        return text.replace(Regex("&[0-9a-fk-or]"), "")
    }
}
