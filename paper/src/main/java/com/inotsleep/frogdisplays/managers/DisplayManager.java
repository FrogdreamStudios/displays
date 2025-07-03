package com.inotsleep.frogdisplays.managers;

import com.inotsleep.frogdisplays.FrogdisplaysPlugin;
import com.inotsleep.frogdisplays.datatypes.DisplayData;
import com.inotsleep.frogdisplays.datatypes.SelectionData;
import com.inotsleep.frogdisplays.utils.MessageUtil;
import com.inotsleep.frogdisplays.utils.ReportSender;
import com.inotsleep.frogdisplays.utils.net.PacketUtils;
import me.inotsleep.utils.logging.LoggingManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

///  Manages the display data, including registration, updates, deletion, and reporting.
public class DisplayManager {
    private static final Map<UUID, DisplayData> displays = new HashMap<>();
    private static final Map<UUID, Long> reportTime = new HashMap<>();

    public static DisplayData getDisplayData(UUID id) {
        return displays.get(id);
    }

    public static void register(DisplayData displayData) {
        displays.put(displayData.getId(), displayData);

        List<Player> receivers = displayData.getReceivers();

        displayData.sendUpdatePacket(receivers);
    }

    public static void updateAllDisplays() {
        Map<World, List<Player>> playersByWorld = displays.values().stream()
                .map(DisplayData::getPos1)
                .map(Location::getWorld)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        World::getPlayers,
                        (list1, list2) -> list1
                ));

        for (DisplayData display : displays.values()) {
            World world = display.getPos1().getWorld();
            List<Player> worldPlayers = playersByWorld.computeIfAbsent(world, k -> Collections.emptyList());

            List<Player> receivers = worldPlayers.stream()
                    .filter(player -> display.isInRange(player.getLocation()))
                    .toList();

            display.sendUpdatePacket(receivers);
        }
    }

    public static void delete(DisplayData displayData) {
        if (displayData == null) return;

        new BukkitRunnable() {
            public void run() {
                FrogdisplaysPlugin.getInstance().storage.deleteDisplay(displayData);
            }
        }.runTaskAsynchronously(FrogdisplaysPlugin.getInstance());

        PacketUtils.sendDeletePacket(displayData.getReceivers(), displayData.getId());
        displays.remove(displayData.getId());
    }

    public static void delete(UUID id, Player player) {
        DisplayData displayData = displays.get(id);

        if (displayData == null) return;

        if (!(displayData.getOwnerId() + "").equals(player.getUniqueId() + "")) {
            LoggingManager.warn("Player " + player.getName() + " sent delete packet while he not owner! ");
            return;
        }

       delete(displayData);
    }

    public static void report(UUID id, Player player) {
        DisplayData displayData = displays.get(id);
        if (displayData == null) return;

        long lastReport = reportTime.computeIfAbsent(id, (k) -> 0L);

        if (System.currentTimeMillis() - lastReport < FrogdisplaysPlugin.config.settings.reportCooldown) {
            MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.reportTooQuickly);
            return;
        }

        reportTime.put(id, System.currentTimeMillis());

        new BukkitRunnable() {
            public void run() {
                try {
                    if (Objects.equals(FrogdisplaysPlugin.config.settings.webhookUrl, "")) return;
                    ReportSender.sendReport(displayData.getPos1(), displayData.getUrl(), displayData.getId(), player,  FrogdisplaysPlugin.config.settings.webhookUrl, Bukkit.getOfflinePlayer(displayData.getOwnerId()).getName());
                    MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.reportSent);
                } catch (Exception e) {
                    LoggingManager.error("Unable to send webhook message", e);
                }
            }
        }.runTaskAsynchronously(FrogdisplaysPlugin.getInstance());
    }

    public static boolean isOverlaps(SelectionData data) {
        World selWorld = data.getPos1().getWorld();

        // Vectors from positions
        int minX = Math.min(data.getPos1().getBlockX(), data.getPos2().getBlockX());
        int minY = Math.min(data.getPos1().getBlockY(), data.getPos2().getBlockY());
        int minZ = Math.min(data.getPos1().getBlockZ(), data.getPos2().getBlockZ());
        int maxX = Math.max(data.getPos1().getBlockX(), data.getPos2().getBlockX()) + 1;
        int maxY = Math.max(data.getPos1().getBlockY(), data.getPos2().getBlockY()) + 1;
        int maxZ = Math.max(data.getPos1().getBlockZ(), data.getPos2().getBlockZ()) + 1;

        BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

        for (DisplayData display : displays.values()) {
            if (!display.getPos1().getWorld().equals(selWorld)) continue;

            if (box.overlaps(display.box)) {
                return true;
            }
        }
        return false;
    }

    public static DisplayData isContains(Location location) {
        for (DisplayData display : displays.values()) {
            if (display.getPos1().getWorld() == location.getWorld() && display.box.contains(location.toVector())) return display;
        }

        return null;
    }

    public static void register(List<DisplayData> list) {
        for (DisplayData display : list) {
            displays.put(display.getId(), display);
        }
    }

    public static void save(Consumer<DisplayData> saveDisplay) {
        displays.values().forEach(saveDisplay);
    }
}