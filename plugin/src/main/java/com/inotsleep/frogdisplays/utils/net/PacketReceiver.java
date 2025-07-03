package com.inotsleep.frogdisplays.utils.net;

import com.github.zafarkhaja.semver.Version;
import com.inotsleep.frogdisplays.FrogdisplaysPlugin;
import com.inotsleep.frogdisplays.datatypes.SyncPacket;
import com.inotsleep.frogdisplays.managers.DisplayManager;
import com.inotsleep.frogdisplays.managers.PlayStateManager;
import com.inotsleep.frogdisplays.managers.PlayerManager;
import com.inotsleep.frogdisplays.utils.Utils;
import me.inotsleep.utils.logging.LoggingManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class PacketReceiver implements PluginMessageListener {
    FrogdisplaysPlugin plugin;

    public PacketReceiver(FrogdisplaysPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        switch (channel) {
            case "frogdisplays:sync": {
                processSyncPacket(player, message);
                break;
            }
            case "frogdisplays:req_sync":
            case "frogdisplays:delete":
            case "frogdisplays:report": {
                UUID id = processUUIDPacketWithException(message);

                if (id == null) return;

                switch (channel.split(":")[1]) {
                    case "req_sync": {
                        PlayStateManager.sendSyncPacket(id, player);
                        break;
                    }
                    case "delete": {
                        DisplayManager.delete(id, player);
                    }
                    case "report": {
                        DisplayManager.report(id, player);
                    }
                }
                break;
            }
            case "frogdisplays:version": {
                processVersionPacket(player, message);
            }
        }
    }

    private void processVersionPacket(Player player, byte[] message) {
        if (FrogdisplaysPlugin.modVersion == null) return;
        try {

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            int len = PacketUtils.readVarInt(in);

            byte[] data = new byte[len];

            in.read(data, 0, len);

            PacketUtils.sendPremiumPacket(player, player.hasPermission(FrogdisplaysPlugin.config.permissions.premiumPermission));

            String version = Utils.sanitize(new String(data, 0, len));

            LoggingManager.log(player.getName() + " has Frog Displays with version: " + version +". Premium: " + player.hasPermission(FrogdisplaysPlugin.config.permissions.premiumPermission));

            Version userVersion = Version.parse(version);

            PlayerManager.setVersion(player, userVersion);

            int result = userVersion.compareTo(FrogdisplaysPlugin.modVersion);
            if (result < 0) {
                TextComponent text =
                        Component
                        .text()
                        .content(
                                ChatColor.translateAlternateColorCodes('&', String.format(
                                        FrogdisplaysPlugin.config.messages.newVersion,
                                        FrogdisplaysPlugin.modVersion.toString()
                                ))
                        ).clickEvent(
                                ClickEvent
                                        .clickEvent(
                                                ClickEvent.Action.OPEN_URL,
                                                String.format(
                                                        // TODO: maybe we should use config also for this
                                                        "https://github.com/%s/%s/releases",
                                                        FrogdisplaysPlugin.config.settings.repoOwner,
                                                        FrogdisplaysPlugin.config.settings.repoName
                                                )
                                        )
                        )
                        .build();
                player.sendMessage(text);
            }

        } catch (IOException e) {
            LoggingManager.warn( "Unable to decode SyncPacket", e);
        }
    }

    private void processSyncPacket(Player player, byte[] message) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            UUID id = PacketUtils.readUUID(in);

            boolean isSync = in.readBoolean();
            boolean currentState = in.readBoolean();

            long currentTime = PacketUtils.readVarLong(in);
            long limitTime = PacketUtils.readVarLong(in);

            SyncPacket packet = new SyncPacket(id, isSync, currentState, currentTime, limitTime);
            PlayStateManager.processSyncPacket(packet, player);
        } catch (IOException e) {
            LoggingManager.warn("Unable to decode SyncPacket", e);
        }
    }

    private UUID processUUIDPacketWithException(byte[] message) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            return PacketUtils.readUUID(in);
        } catch (IOException e) {
            LoggingManager.error("Unable to decode RequestSyncPacket", e);
        }
        return null;
    }
}