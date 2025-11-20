package com.dreamdisplays;

import com.dreamdisplays.net.*;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import com.dreamdisplays.downloader.GstreamerDownloadInit;
import com.dreamdisplays.net.*;
import com.dreamdisplays.screen.DisplayConfScreen;
import com.dreamdisplays.screen.Screen;
import com.dreamdisplays.screen.ScreenManager;
import com.dreamdisplays.util.Facing;
import com.dreamdisplays.util.RCUtil;
import com.dreamdisplays.util.Utils;

import java.io.File;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlatformlessInitializer {

    public static Config config;

    public static Thread timerThread = new Thread(() -> {
        int lastDistance = 64;
        boolean isErrored = false;
        while (!isErrored) {
            ScreenManager.getScreens().forEach(Screen::reloadQuality);
            if (
                    config.defaultDistance != lastDistance
            ) {
                config.defaultDistance = lastDistance;
                config.save();
            }
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                isErrored = true;
            }
        }
    });

    public static boolean isOnScreen = false;
    public static boolean focusMode = false;

    public static boolean displaysEnabled = true;

    public static Config getConfig() {
        return config;
    }

    public static final String MOD_ID = "dreamdisplays";

    private static Screen hoveredScreen = null;

    private static Mod mod;

    public static void onModInit(Mod DreamDisplaysMod) {
        mod = DreamDisplaysMod;
        LoggingManager.setLogger(LoggerFactory.getLogger(MOD_ID));
        LoggingManager.info("Starting Dream Displays");

        config = new Config(new File("./config/" + MOD_ID));
        config.reload();

        GstreamerDownloadInit.init();
        new WindowFocusMuteThread().start();

        timerThread.start();
    }

    public static void onDisplayInfoPacket(DisplayInfoPacket payload) {
        if (!PlatformlessInitializer.displaysEnabled) return;

        if (ScreenManager.screens.containsKey(payload.id())) {
            Screen screen = ScreenManager.screens.get(payload.id());
            screen.updateData(payload);
            return;
        }

        createScreen(payload.id(), payload.ownerId(), payload.pos(), payload.facing(), payload.width(), payload.height(), payload.url(), payload.lang(), payload.isSync());
    }

    public static void createScreen(UUID id, UUID ownerId, Vector3i pos, Facing facing, int width, int height, String code, String lang, boolean isSync) {
        Screen screen = new Screen(id, ownerId, pos.x(), pos.y(), pos.z(), facing.toString(), width, height, isSync);
        assert Minecraft.getInstance().player != null;
        if (screen.getDistanceToScreen(Minecraft.getInstance().player.blockPosition()) > PlatformlessInitializer.config.defaultDistance) return;
        ScreenManager.registerScreen(screen);
        if (!Objects.equals(code, "")) screen.loadVideo(code, lang);
    }

    public static void onSyncPacket(SyncPacket payload) {
        if (!ScreenManager.screens.containsKey(payload.id())) return;
        Screen screen = ScreenManager.screens.get(payload.id());
        screen.updateData(payload);
    }

    private static final boolean[] wasPressed = {false};
    private static final AtomicBoolean wasInMultiplayer = new AtomicBoolean(false);
    private static final AtomicReference<ClientLevel> lastWorld = new AtomicReference<>(null);
    private static final AtomicBoolean wasFocused = new AtomicBoolean(false);

    private static void checkVersionAndSendPacket() {
        try {
            String version = Utils.readResource("/version");
            sendPacket(new VersionPacket(version));
        } catch (Exception e) {
            LoggingManager.error("Unable to get version", e);
        }
    }

    public static void onEndTick(Minecraft client) {
        if (client.level != null && client.getCurrentServer() != null) {
            if (lastWorld.get() == null) {
                lastWorld.set(client.level);
                checkVersionAndSendPacket();
            }

            if (client.level != lastWorld.get()) {
                lastWorld.set(client.level);

                ScreenManager.unloadAll();
                hoveredScreen = null;

                checkVersionAndSendPacket();

            }

            wasInMultiplayer.set(true);
        } else {
            if (wasInMultiplayer.get()) {
                wasInMultiplayer.set(false);
                ScreenManager.unloadAll();
                hoveredScreen = null;
                lastWorld.set(null);
                return;
            }
        }

        if (client.player == null) return;

        BlockHitResult result = RCUtil.rCBlock(64);
        hoveredScreen = null;
        PlatformlessInitializer.isOnScreen = false;

        for (Screen screen : ScreenManager.getScreens()) {
            if (PlatformlessInitializer.config.defaultDistance < screen.getDistanceToScreen(client.player.blockPosition()) || !PlatformlessInitializer.displaysEnabled) {
                ScreenManager.unregisterScreen(screen);
                if (hoveredScreen == screen) {
                    hoveredScreen = null;
                    PlatformlessInitializer.isOnScreen = false;
                }
            } else {
                if (result != null) if (screen.isInScreen(result.getBlockPos())) {
                    hoveredScreen = screen;
                    PlatformlessInitializer.isOnScreen = true;
                }

                screen.tick(client.player.blockPosition());
            }
        }

        long window = client.getWindow().handle();
        boolean pressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (pressed && !wasPressed[0]) {
            if (client.player != null && client.player.isShiftKeyDown()) {
                checkAndOpenScreen();
            }
        }

        wasPressed[0] = pressed;

        if (PlatformlessInitializer.focusMode && client.player != null && hoveredScreen != null) {
            client.player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    20 * 2,
                    1,
                    false,
                    false,
                    false
            ));

            wasFocused.set(true);

        } else if (!PlatformlessInitializer.focusMode && wasFocused.get() && client.player != null) {
            client.player.removeEffect(MobEffects.BLINDNESS);
            wasFocused.set(false);
        }
    }

    private static void checkAndOpenScreen() {
        if (hoveredScreen == null) return;
        DisplayConfScreen.open(hoveredScreen);
    }

    public static void sendPacket(CustomPacketPayload packet) {
        mod.sendPacket(packet);
    }

    public static void onDeletePacket(DeletePacket deletePacket) {
        Screen screen = ScreenManager.screens.get(deletePacket.id());
        if (screen == null) return;

        ScreenManager.unregisterScreen(screen);
    }

    public static void onStop() {
        timerThread.interrupt();
        ScreenManager.unloadAll();
        WindowFocusMuteThread.instance.interrupt();
    }

    public static boolean isPremium = false;

    public static void onPremiumPacket(PremiumPacket payload) {
        isPremium = payload.premium();
    }
}