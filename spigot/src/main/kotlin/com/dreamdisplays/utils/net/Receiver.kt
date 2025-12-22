package com.dreamdisplays.utils.net

import com.dreamdisplays.Main
import com.dreamdisplays.datatypes.SyncData
import com.dreamdisplays.managers.DisplayManager
import com.dreamdisplays.managers.DisplayManager.delete
import com.dreamdisplays.managers.DisplayManager.report
import com.dreamdisplays.managers.PlayerManager.hasBeenNotifiedAboutModUpdate
import com.dreamdisplays.managers.PlayerManager.hasBeenNotifiedAboutPluginUpdate
import com.dreamdisplays.managers.PlayerManager.setModUpdateNotified
import com.dreamdisplays.managers.PlayerManager.setPluginUpdateNotified
import com.dreamdisplays.managers.PlayerManager.setVersion
import com.dreamdisplays.managers.StateManager.processSyncPacket
import com.dreamdisplays.managers.StateManager.sendSyncPacket
import com.dreamdisplays.utils.Message
import com.dreamdisplays.utils.Utils
import com.github.zafarkhaja.semver.Version
import com.google.gson.Gson
import me.inotsleep.utils.logging.LoggingManager
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.jspecify.annotations.NullMarked
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.util.*
import com.dreamdisplays.utils.net.Utils as Net

@NullMarked
class Receiver(var plugin: Main?) : PluginMessageListener {
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        when (channel) {
            "dreamdisplays:sync" -> {
                processSyncPacket(player, message)
            }

            "dreamdisplays:req_sync", "dreamdisplays:delete", "dreamdisplays:report" -> {
                val id = processUUIDPacketWithException(message) ?: return

                when (channel.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]) {
                    "req_sync" -> {
                        sendSyncPacket(id, player)
                    }

                    "delete" -> {
                        delete(id, player)
                        Message.sendMessage(player, "displayDeleted")
                    }

                    "report" -> {
                        report(id, player)
                    }
                }
            }

            "dreamdisplays:version" -> {
                processVersionPacket(player, message)
            }

            "dreamdisplays:display_enabled" -> {
                processDisplayEnabledPacket(player, message)
            }
        }
    }

    private fun processVersionPacket(player: Player, message: ByteArray) {
        if (Main.modVersion == null) return
        try {
            val `in` = DataInputStream(ByteArrayInputStream(message))
            val len = Net.readVarInt(`in`)

            val data = ByteArray(len)

            `in`.read(data, 0, len)

            Net.sendPremiumPacket(
                player,
                player.hasPermission(Main.config.permissions.premium)
            )

            // Send report enabled status
            Net.sendReportEnabledPacket(player, Main.config.settings.webhookUrl.isNotEmpty())

            // Send all existing displays to the player
            sendAllDisplaysToPlayer(player)

            // Store user version
            val version = Utils.sanitize(String(data, 0, len))

            // Logging player version info
            LoggingManager.log(player.name + " joined with Dream Displays " + version)

            val userVersion = Version.parse(version)

            setVersion(player, userVersion)

            // Check for mod updates and notify all users with the mod
            val result = userVersion.compareTo(Main.modVersion)
            if (result < 0 && !hasBeenNotifiedAboutModUpdate(player)) {
                val rawMessage = Main.config.messages["newVersion"]
                val processedMessage = if (rawMessage is String) {
                    String.format(rawMessage, Main.modVersion.toString())
                } else {
                    val gson = Gson()
                    val jsonStr = gson.toJson(rawMessage)
                    val component = GsonComponentSerializer.gson().deserialize(jsonStr)
                    component.replaceText(
                        TextReplacementConfig.builder().matchLiteral("%s").replacement(Main.modVersion.toString())
                            .build()
                    )
                }
                Message.sendColoredMessage(player, processedMessage)
                setModUpdateNotified(player, true)
            }

            // Check for plugin updates and notify admins only
            if (Main.config.settings.updatesEnabled &&
                player.hasPermission(Main.config.permissions.updates) && !hasBeenNotifiedAboutPluginUpdate(
                    player
                )
            ) {
                val pluginVersion: String = Main.getInstance().description.version
                if (Main.pluginLatestVersion != null) {
                    val currentPluginVersion = Version.parse(pluginVersion)
                    val latestPluginVersion = Version.parse(Main.pluginLatestVersion)

                    if (currentPluginVersion < latestPluginVersion) {
                        val message = Main.config.messages["newPluginVersion"] as? String
                            ?: "&7D |&f New version of Dream Displays plugin (%s) is available! Please update the server plugin!"
                        Message.sendColoredMessage(player, String.format(message, Main.pluginLatestVersion))
                        setPluginUpdateNotified(player, true)
                    }
                }
            }
        } catch (e: IOException) {
            LoggingManager.warn("Unable to decode VersionPacket", e)
        }
    }

    private fun processSyncPacket(player: Player, message: ByteArray) {
        try {
            val `in` = DataInputStream(ByteArrayInputStream(message))
            val id = Net.readUUID(`in`)

            val isSync = `in`.readBoolean()
            val currentState = `in`.readBoolean()

            val currentTime = Net.readVarLong(`in`)
            val limitTime = Net.readVarLong(`in`)

            val packet = SyncData(id, isSync, currentState, currentTime, limitTime)
            processSyncPacket(packet, player)
        } catch (e: IOException) {
            LoggingManager.warn("Unable to decode SyncPacket", e)
        }
    }

    private fun processUUIDPacketWithException(message: ByteArray): UUID? {
        try {
            val `in` = DataInputStream(ByteArrayInputStream(message))
            return Net.readUUID(`in`)
        } catch (e: IOException) {
            LoggingManager.error("Unable to decode RequestSyncPacket", e)
        }
        return null
    }

    private fun processDisplayEnabledPacket(player: Player, message: ByteArray) {
        try {
            val `in` = DataInputStream(ByteArrayInputStream(message))
            val enabled = `in`.readBoolean()
            com.dreamdisplays.managers.PlayerManager.setDisplaysEnabled(player, enabled)
        } catch (e: IOException) {
            LoggingManager.warn("Unable to decode DisplayEnabledPacket", e)
        }
    }

    private fun sendAllDisplaysToPlayer(player: Player) {
        val displays = DisplayManager.getDisplays()
        val playerList = mutableListOf<Player?>(player)

        for (display in displays) {
            // Only send displays from the same world as the player
            if (display.pos1.world != player.world) continue

            Net.sendDisplayInfoPacket(
                playerList,
                display.id,
                display.ownerId,
                display.box.min,
                display.width,
                display.height,
                display.url,
                display.lang,
                display.facing,
                display.isSync
            )
        }
    }
}
