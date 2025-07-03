package com.inotsleep.frogdisplays.listeners;

import com.inotsleep.frogdisplays.FrogdisplaysPlugin;
import com.inotsleep.frogdisplays.datatypes.SelectionData;
import com.inotsleep.frogdisplays.managers.DisplayManager;
import com.inotsleep.frogdisplays.utils.MessageUtil;
import com.inotsleep.frogdisplays.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/// Listener for handling player interactions related to display selection in the Frog Displays plugin.
/// This class listens for player clicks on blocks to select points for display creation, manages selection data, and validates the selection.
///
/// Handles events such as block breaks, explosions, and piston movements to ensure the integrity of the display selections.
public class SelectionListener implements Listener {
    public static Map<UUID, SelectionData> selectionPoints = new HashMap<>();

    public SelectionListener(FrogdisplaysPlugin plugin) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                selectionPoints.forEach((key, value) -> value.drawBox());
            }
        };

        runnable.runTaskTimer(plugin, 0, FrogdisplaysPlugin.config.settings.particleRenderDelay);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() != FrogdisplaysPlugin.config.settings.selectionMaterial) return;

        if (player.isSneaking() && event.getAction().isRightClick() && selectionPoints.containsKey(player.getUniqueId())) {
            selectionPoints.remove(player.getUniqueId());
            MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.selectionClear);
            return;
        }

        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != FrogdisplaysPlugin.config.settings.baseMaterial) return;

        event.setCancelled(true);

        Location location = event.getClickedBlock().getLocation();
        SelectionData data = selectionPoints.getOrDefault(player.getUniqueId(), new SelectionData(player));

        if (
                data.getPos1() != null && data.getPos1().getWorld() != location.getWorld() ||
                data.getPos2() != null && data.getPos2().getWorld() != location.getWorld()
        ) {
            data.setPos1(null);
            data.setPos2(null);
        }

        data.setReady(false);

        BlockFace face = event.getBlockFace();

        if (face == BlockFace.UP || face == BlockFace.DOWN) face = player.getFacing().getOppositeFace();

        if (event.getAction().isLeftClick()) {
            data.setPos1(location.clone());
            data.setFace(face);
            MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.firstPointSelected);

            int validationCode = isValidDisplay(data);
            if (validationCode == 6) data.setReady(true);
        } else if (event.getAction().isRightClick()) {
            if (data.getPos1() == null) {
                MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.noDisplayTerritories);
                return;
            }
            data.setPos2(location.clone());
            MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.secondPointSelected);

            int validationCode = isValidDisplay(data);
            if (validationCode != 6) {
                data.setReady(false);
                selectionPoints.put(player.getUniqueId(), data);

                sendErrorMessage(player, validationCode);

                return;
            }

            data.setReady(true);
            MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.createDisplayCommand);
        }

        selectionPoints.put(player.getUniqueId(), data);
    }

    public static void sendErrorMessage(Player player, int validationCode) {
        switch (validationCode) {
            case 0:
                MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.secondPointNotSelected);
                break;
            case 1:
                MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.displayOverlap);
                break;
            case 2:
                MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.structureWrongDepth);
                break;
            case 3:
                MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.structureTooSmall);
                break;
            case 4:
                MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.structureTooLarge);
                break;
            case 5:
                MessageUtil.sendColoredMessage(player, FrogdisplaysPlugin.config.messages.wrongStructure);
                break;
        }
    }

    public static int isValidDisplay(SelectionData data) {
        Location pos1 = data.getPos1();
        Location pos2 = data.getPos2();

        if (pos1 == null || pos2 == null || data.getFace() == null) return 0;

        if (pos1.getWorld() != pos2.getWorld()) return 1;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int deltaX = Math.abs(pos1.getBlockX() - pos2.getBlockX())+1;
        int deltaZ = Math.abs(pos1.getBlockZ() - pos2.getBlockZ())+1;

        if (deltaX != Math.abs(data.getFace().getModX()) && deltaZ != Math.abs(data.getFace().getModZ())) return 2;

        int width = Math.max(deltaX, deltaZ);
        int height = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;

        if (height < FrogdisplaysPlugin.config.settings.minHeight || width < FrogdisplaysPlugin.config.settings.minWidth) return 3;
        if (height > FrogdisplaysPlugin.config.settings.maxHeight || width > FrogdisplaysPlugin.config.settings.maxWidth) return 4;

        Material requiredMaterial = FrogdisplaysPlugin.config.settings.baseMaterial;

        World world = pos1.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != requiredMaterial) {
                        return 5;
                    }
                }
            }
        }

        return 6;
    }

    //

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockDestroy(event.getBlock().getType(), event.getBlock().getLocation(), event);
    }

    @EventHandler
    public void onExplodeEvent(EntityExplodeEvent event) {
        List<Block> toRemove = new ArrayList<>();
        List<SelectionData> dataList = selectionPoints.values().stream().filter(SelectionData::isReady).toList();

        event.blockList().stream().filter(block -> block.getType() == FrogdisplaysPlugin.config.settings.baseMaterial).forEach(block -> {
            if (block.getType() != FrogdisplaysPlugin.config.settings.baseMaterial) return;
            Location location = block.getLocation();

            if (DisplayManager.isContains(location) != null) {
                toRemove.add(block);
                return;
            }

            for (SelectionData data : dataList) {
                if (Utils.isInBoundaries(data.getPos1(), data.getPos2(), location)) {
                    toRemove.add(block);
                    break;
                }
            }
        });

        event.blockList().removeAll(toRemove);
    }

    private void handleBlockDestroy(Material material, Location location, Cancellable event) {
        if (material != FrogdisplaysPlugin.config.settings.baseMaterial) return;
        if (DisplayManager.isContains(location) != null) event.setCancelled(true);

        for (Map.Entry<UUID, SelectionData> eData : selectionPoints.entrySet()) {
            UUID uuid = eData.getKey();
            SelectionData data = eData.getValue();

            if (!data.isReady() || Bukkit.getPlayer(uuid) != null) continue;

            if (Utils.isInBoundaries(data.getPos1(), data.getPos2(), location)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onBlockPush(BlockPistonExtendEvent event) {
        handlePistonEvent(event.getBlocks(), event);
    }

    @EventHandler
    public void onBlockPull(BlockPistonRetractEvent event) {
        handlePistonEvent(event.getBlocks(), event);
    }

    private void handlePistonEvent(List<Block> blocks, Cancellable event) {
        List<SelectionData> dataList = selectionPoints.values().stream().filter(SelectionData::isReady).toList();

        blocks.stream().filter(block -> block.getType() == FrogdisplaysPlugin.config.settings.baseMaterial).forEach(block -> {
            if (event.isCancelled()) return;
            if (block.getType() != FrogdisplaysPlugin.config.settings.baseMaterial) return;

            Location location = block.getLocation();

            if (DisplayManager.isContains(location) != null) {
                event.setCancelled(true);
                return;
            }

            for (SelectionData data : dataList) {
                if (Utils.isInBoundaries(data.getPos1(), data.getPos2(), location)) {
                    event.setCancelled(true);
                    break;
                }
            }
        });
    }
}