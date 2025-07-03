package ru.l0sty.dreamdisplays;

import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.hit.BlockHitResult;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import ru.l0sty.dreamdisplays.downloader.GstreamerDownloadInit;
import ru.l0sty.dreamdisplays.net.*;
import ru.l0sty.dreamdisplays.screen.DisplayConfScreen;
import ru.l0sty.dreamdisplays.screen.Screen;
import ru.l0sty.dreamdisplays.screen.ScreenManager;
import ru.l0sty.dreamdisplays.util.Facing;
import ru.l0sty.dreamdisplays.util.RCUtil;
import ru.l0sty.dreamdisplays.util.Utils;

import java.io.File;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/// This class is the main entry point for the Dream Displays mod.
/// It initializes the mod, handles packets, and manages screens.
/// It also provides utility methods for sending packets and managing the configuration.
public class PlatformlessInitializer {
    public static Config config;

    public static boolean isOnScreen = false;
    public static boolean focusMode = false;

    public static double maxDistance = 64;
    public static boolean displaysEnabled = true;

    public static Config getConfig() {
        return config;
    }

    public static final String MOD_ID = "dreamdisplays";

    public static String proxyAddress = "";

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
        assert MinecraftClient.getInstance().player != null;
        if (screen.getDistanceToScreen(MinecraftClient.getInstance().player.getBlockPos()) > PlatformlessInitializer.maxDistance) return;
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
    private static final AtomicReference<ClientWorld> lastWorld = new AtomicReference<>(null);
    private static final AtomicBoolean wasFocused = new AtomicBoolean(false);

    private static void checkVersionAndSendPacket() {
        try {
            String version = Utils.readResource("/version");
            sendPacket(new VersionPacket(version));
        } catch (Exception e) {
            LoggingManager.error("Unable to get version", e);
        }
    }

    public static void onEndTick(MinecraftClient client) {
        if (client.world != null && client.getCurrentServerEntry() != null) {
            if (lastWorld.get() == null) {
                lastWorld.set(client.world);
                checkVersionAndSendPacket();
            }

            if (client.world != lastWorld.get()) {
                lastWorld.set(client.world);

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
            if (PlatformlessInitializer.maxDistance < screen.getDistanceToScreen(client.player.getBlockPos()) || !PlatformlessInitializer.displaysEnabled) {
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

                screen.tick(client.player.getBlockPos());
            }
        }

        long window = client.getWindow().getHandle();
        boolean pressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (pressed && !wasPressed[0]) {
            if (client.player != null && client.player.isSneaking()) {
                checkAndOpenScreen();
            }
        }

        wasPressed[0] = pressed;

        if (PlatformlessInitializer.focusMode && client.player != null && hoveredScreen != null) {
            client.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS,
                    20 * 2,
                    1,
                    false,
                    false,
                    false
            ));

            wasFocused.set(true);

        } else if (!PlatformlessInitializer.focusMode && wasFocused.get() && client.player != null) {
            client.player.removeStatusEffect(StatusEffects.BLINDNESS);
            wasFocused.set(false);
        }
    }

    private static void checkAndOpenScreen() {
        if (hoveredScreen == null) return;
        DisplayConfScreen.open(hoveredScreen);
    }

    public static void sendPacket(CustomPayload packet) {
        mod.sendPacket(packet);
    }

    public static void onDeletePacket(DeletePacket deletePacket) {
        Screen screen = ScreenManager.screens.get(deletePacket.id());
        if (screen == null) return;

        ScreenManager.unregisterScreen(screen);
    }

    public static boolean isPremium = false;

    public static void onPremiumPacket(PremiumPacket payload) {
        isPremium = payload.premium();
    }
}