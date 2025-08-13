package com.inotsleep.dreamdisplays.client_1_21_8;

import com.inotsleep.dreamdisplays.client.ClientMod;
import com.inotsleep.dreamdisplays.client.ClientModHolder;
import com.inotsleep.dreamdisplays.client.Config;
import com.inotsleep.dreamdisplays.client.DisplayManager;
import com.inotsleep.dreamdisplays.client.display.Display;
import com.inotsleep.dreamdisplays.client.downloader.Downloader;
import com.inotsleep.dreamdisplays.client.utils.Utils;
import com.inotsleep.dreamdisplays.client_1_21_8.packets.*;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.DisplayConfigurationScreen;
import com.inotsleep.dreamdisplays.client_1_21_8.util.ClientUtils;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

public class DreamDisplaysClientCommon implements ClientMod {
    public static final String MOD_ID = "dreamdisplays";
    public volatile static boolean isOnScreen;

    private static PacketSender packetSender;

    public static void onModInit(PacketSender packetSender) {
        DreamDisplaysClientCommon.packetSender = packetSender;
        ClientModHolder.initialize(new DreamDisplaysClientCommon());

        new Config(new File("./config/" + MOD_ID)).reload();
    }

    public static void onDisplayInfoPacket(DisplayInfoPacket payload) {
        Display display = DisplayManager.getDisplay(payload.id());

        boolean isNew = display == null;

        if (isNew) {
            display = new Display(
                    payload.id(),
                    payload.ownerId(),
                    payload.facing(),
                    payload.x(),
                    payload.y(),
                    payload.z(),
                    payload.width(),
                    payload.height()
            );
        }

        String videoCode = Utils.extractVideoId(payload.videoCode());

        System.out.println(videoCode);
        System.out.println(payload.videoCode());

        display.setVideoCode(videoCode);
        display.setLanguage(payload.lang());
        display.setSync(payload.isSync());

        if (isNew) {
            DisplayManager.addDisplay(display);
            display.startPlayer();
            display.setPaused(false);
        }
    }

    public static void onPremiumPacket(PremiumPacket payload) {

    }

    public static void onDeletePacket(DeletePacket deletePacket) {

    }

    public static void onSyncPacket(SyncPacket payload) {

    }

    private static Level lastLevel = null;
    private static Display hoveredDisplay = null;
    static boolean wasDown = false;

    public static void onTick() {
        Minecraft client = Minecraft.getInstance();

        if (lastLevel != client.level) {
            lastLevel = client.level;

            if (lastLevel != null) {
                checkVersionAndSendPacket();

                Path savePath = ClientUtils.getClientSettingSavePath();

                if (savePath != null) DisplayManager.setSavePath(savePath);
            } else {
                DisplayManager.saveSettings();
                DisplayManager.closeDisplays();
                Config.getInstance().save();
            }
        }

        if (client.player == null || client.level == null) return;

        hoveredDisplay = null;
        isOnScreen = false;

        Vec3 pos = client.player.position();

        BlockPos rayCastResult = ClientUtils.rayCast(Config.getInstance().renderDistance);

        for (Display display : DisplayManager.getDisplays()) {
            if (
                    Config.getInstance().displaysDisabled ||
                    display.getDistance(pos.x, pos.y, pos.z) > Config.getInstance().renderDistance
            ) {
                DisplayManager.removeDisplay(display.getId());
                if (hoveredDisplay == display) {
                    hoveredDisplay = null;
                    isOnScreen = false;
                }
            } else if (rayCastResult != null) {
                if (display.isInScreen(rayCastResult.getX(), rayCastResult.getY(), rayCastResult.getZ())) {
                    hoveredDisplay = display;
                    isOnScreen = true;
                }

                display.tick(pos.x, pos.y, pos.z);
            }
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options.keyUse.isDown() && !wasDown) {
            wasDown = true;
        } else if (!minecraft.options.keyUse.isDown() && wasDown) {
            wasDown = false;
        }

        if (hoveredDisplay != null) {

            minecraft.setScreen(new DisplayConfigurationScreen(hoveredDisplay));

        }
    }

    private static void checkVersionAndSendPacket() {
        try {
            LoggingManager.info("Checking version...");
            String version = Utils.readResource("/version");
            LoggingManager.info(version);
            packetSender.sendPacket(new VersionPacket(version));
        } catch (Exception e) {
            LoggingManager.error("Unable to get version", e);
        }
    }

    @Override
    public void sendSyncUpdate(UUID id, long time, boolean paused, boolean isSync, long duration) {
        packetSender.sendPacket(new SyncPacket(id, isSync, paused, time, duration));
    }

    @Override
    public void sendRequestSync(UUID id) {
        packetSender.sendPacket(new RequestSyncPacket(id));
    }

    @Override
    public void sendDeletePacket(UUID id) {
        packetSender.sendPacket(new DeletePacket(id));
    }

    @Override
    public void sendReportPacket(UUID id) {
        packetSender.sendPacket(new ReportPacket(id));
    }

    @Override
    public UUID getPlayerID() {
        if (Minecraft.getInstance().player == null) return null;
        return Minecraft.getInstance().player.getUUID();
    }

    @Override
    public boolean isFocused() {
        return Minecraft.getInstance().isWindowActive();
    }
}
