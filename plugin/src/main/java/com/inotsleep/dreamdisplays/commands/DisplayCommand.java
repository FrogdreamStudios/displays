package com.inotsleep.dreamdisplays.commands;

import com.github.zafarkhaja.semver.Version;
import com.inotsleep.dreamdisplays.DreamDisplaysPlugin;
import com.inotsleep.dreamdisplays.datatypes.DisplayData;
import com.inotsleep.dreamdisplays.datatypes.SelectionData;
import com.inotsleep.dreamdisplays.listeners.SelectionListener;
import com.inotsleep.dreamdisplays.managers.DisplayManager;
import com.inotsleep.dreamdisplays.managers.PlayerManager;
import com.inotsleep.dreamdisplays.utils.MessageUtil;
import com.inotsleep.dreamdisplays.utils.Utils;
import me.inotsleep.utils.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Commands related to the display management system.
/// This class handles commands for creating, deleting, and managing displays in the Dream Displays plugin.
public class DisplayCommand extends AbstractCommand {
    public DisplayCommand() {
        super(DreamDisplaysPlugin.getInstance().getName(), "display");
    }

    /// Executes the command based on the provided arguments.
    /// @param sender the command sender who issued the command.
    /// @param s the command label.
    /// @param args the arguments provided with the command.
    /// @return void
    @Override
    public void toExecute(CommandSender sender, String s, String[] args) {
        switch (args.length) {
            case 0 -> sendHelp(sender);
            case 1 -> handle1Args(sender, args);
            case 2, 3 -> handleVideoCommand(sender, args);
        }
    }

    ///  Handles the video command for setting a YouTube video on a display.
    /// @param sender the command sender who issued the command.
    private void handleVideoCommand(CommandSender sender, String[] args) {
        if (args[0].equals("video")) {
            if (!(sender instanceof Player player)) return;

            Block block = player.getTargetBlock(null, 32);

            if (block.getType() != DreamDisplaysPlugin.config.settings.baseMaterial) {
                MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.noDisplay);
                return;
            }

            Location location = block.getLocation();

            DisplayData data = DisplayManager.isContains(location);
            if (data == null || !(data.getOwnerId() + "").equals(player.getUniqueId() + "")) {
                MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.noDisplay);
                return;
            }

            String code = Utils.extractVideoId(args[1]);

            String lang = args.length == 3 ? args[2] : "";

            if (code == null) {
                MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.invalidURL);
                return;
            }

            data.setUrl("https://youtube.com/watch?v=" + code);
            data.setLang(lang);
            data.setSync(false);
            data.sendUpdatePacket(data.getReceivers());

            MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.settedURL);
        }
    }

    ///  Handles commands with one argument, such as creating, deleting, reloading, or listing displays.
    /// @param sender The command sender who issued the command.
    private void handle1Args(CommandSender sender, String[] args) {
        switch (args[0]) {
            case "create" -> {
                if (!(sender instanceof Player player)) return;

                SelectionData data = SelectionListener.selectionPoints.get(player.getUniqueId());
                if (data == null) {
                    MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.noDisplayTerritories);
                    return;
                }

                int validation = SelectionListener.isValidDisplay(data);

                if (validation != 6) {
                    SelectionListener.sendErrorMessage(player, validation);
                    return;
                }

                if (DisplayManager.isOverlaps(data)) {
                    MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.displayOverlap);
                    return;
                }

                DisplayData displayData = data.generateDisplayData();
                SelectionListener.selectionPoints.remove(player.getUniqueId());

                DisplayManager.register(displayData);

                MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.successfulCreation);
            }
            case "delete" -> {
                if (!(sender instanceof Player player)) return;
                if (!player.hasPermission(DreamDisplaysPlugin.config.permissions.deletePermission)) {
                    return;
                }

                Block block = player.getTargetBlock(null, 32);
                if (block.getType() != DreamDisplaysPlugin.config.settings.baseMaterial) {
                    MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.noDisplay);
                    return;
                }

                Location location = block.getLocation();

                DisplayData data = DisplayManager.isContains(location);
                if (data == null) {
                    MessageUtil.sendColoredMessage(player, DreamDisplaysPlugin.config.messages.noDisplay);
                    return;
                }

                DisplayManager.delete(data);
            }
            case "reload" -> {
                if (!sender.hasPermission("dreamdisplays.reload")) {
                    sendHelp(sender);
                    return;
                }

                DreamDisplaysPlugin.config.reload();
                MessageUtil.sendColoredMessage(sender, "&aDream Displays has been reloaded!");
            }
            case "list" -> {
                if (!sender.hasPermission(DreamDisplaysPlugin.config.permissions.listPermission)) {
                    sendHelp(sender);
                    return;
                }

                Map<Version, Integer> usage = new HashMap<>();

                PlayerManager.getVersions().forEach(version -> {
                    usage.compute(version, (k, v) -> v == null ? 1 : v + 1);
                });

                MessageUtil.sendColoredMessages(
                        sender,
                        DreamDisplaysPlugin.config.messages.usageHeader
                                .stream()
                                .map(
                                        (s) -> me.inotsleep.utils.MessageUtil.parsePlaceholders(
                                                s,
                                                String.valueOf(usage.size()),
                                                String.valueOf(Bukkit.getOnlinePlayers().size()))
                                )
                                .toList()
                );

                usage.forEach(
                        (k, v) -> MessageUtil.sendColoredMessage(
                                sender,
                                me.inotsleep.utils.MessageUtil.parsePlaceholders(
                                        DreamDisplaysPlugin.config.messages.usageVersionEntry,
                                        k.toString(),
                                        String.valueOf(v)
                                )
                        )
                );

                MessageUtil.sendColoredMessage(sender, DreamDisplaysPlugin.config.messages.usageFooter);
            }
        }
    }

    ///  Sends help messages to the command sender.
    private void sendHelp(CommandSender sender) {
        MessageUtil.sendColoredMessages(sender, DreamDisplaysPlugin.config.messages.displayCommandHelp);
    }

    ///  Completes the command with suggestions based on the arguments provided.
    /// @param sender The command sender who issued the command.
    /// @param args The arguments provided with the command.
    /// @return A list of suggestions for command completion.
    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("create", "video"));
            if (sender.hasPermission(DreamDisplaysPlugin.config.permissions.deletePermission)) completions.add("delete");
            if (sender.hasPermission(DreamDisplaysPlugin.config.permissions.listPermission)) completions.add("list");
            return completions;
        }
        return List.of();
    }
}