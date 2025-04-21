package ru.l0sty.frogdisplays;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import ru.l0sty.frogdisplays.net.*;
import ru.l0sty.frogdisplays.render.ScreenWorldRenderer;
import ru.l0sty.frogdisplays.screen.DisplayConfScreen;
import ru.l0sty.frogdisplays.screen.Screen;
import ru.l0sty.frogdisplays.screen.ScreenManager;
import net.fabricmc.api.ClientModInitializer;
import ru.l0sty.frogdisplays.util.Facing;
import ru.l0sty.frogdisplays.util.RaycastUtil;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrogDisplaysMod implements ClientModInitializer {
    public static final String MOD_ID = "frogdisplays";

    public static boolean isOnScreen = false;
    private static FrogDisplaysMod instance;
    public static String proxyAddress = "";

    public static double maxDistance = 64;
    public static boolean displaysEnabled = true;

    private Screen hoveredScreen = null;

    public static FrogDisplaysMod getInstance() {
        return instance;
    }

    public static Config config;

    public static Config getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {


        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("displays")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("off")
                                .executes((context) -> {
                                    displaysEnabled = false;
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("on")
                                .executes((context) -> {
                                    displaysEnabled = true;
                                    return 1;
                                })
                        )
        ));


        testOpenMenu = new KeyBinding(
                "Open menu to test", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, "key.categories.misc"
        );

        instance = this;

        PayloadTypeRegistry.playS2C().register(DisplayInfoPacket.PACKET_ID, DisplayInfoPacket.PACKET_CODEC);


        ClientPlayNetworking.registerGlobalReceiver(DisplayInfoPacket.PACKET_ID, (payload, context) -> {
            if (!displaysEnabled) return;

            if (ScreenManager.screens.containsKey(payload.id())) {
                Screen screen = ScreenManager.screens.get(payload.id());
                screen.updateData(payload);
                return;
            }

            createScreen(payload.id(), payload.pos(), payload.facing(), payload.width(), payload.height(), payload.url());
        });

        ScreenWorldRenderer.register();

        new WindowFocusMuteThread().start();

        final boolean[] wasPressed = {false};
        AtomicBoolean wasInMultiplayer = new AtomicBoolean(false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.getCurrentServerEntry() != null) {
                wasInMultiplayer.set(true);
            } else {
                if (wasInMultiplayer.get()) {
                    wasInMultiplayer.set(false);
                    ScreenManager.unloadAll();
                    hoveredScreen = null;
                    return;
                }
            }

            if (client.player == null) return;

            if (testOpenMenu.isPressed() && hoveredScreen != null) {
                hoveredScreen.waitForMFInit(() -> {
                    hoveredScreen.startVideo();
                    hoveredScreen.setVolume(0);
                });
            }

            BlockHitResult result = RaycastUtil.raycastBlockClient(64);
            hoveredScreen = null;
            isOnScreen = false;

            for (Screen screen : ScreenManager.getScreens()) {
                if (maxDistance < screen.getDistanceToScreen(client.player.getBlockPos()) || !displaysEnabled) ScreenManager.unregisterScreen(screen);

                if (result != null) if (screen.isInScreen(result.getBlockPos())) {
                    hoveredScreen = screen;
                    isOnScreen = true;
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
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ScreenManager.unloadAll();
        });

    }

    private void checkAndOpenScreen() {
        if (hoveredScreen == null) return;
        DisplayConfScreen.open(hoveredScreen);
    }

    void createScreen(UUID id, Vector3i pos, Facing facing, int width, int height, String code) {
        Screen screen = new Screen(id, pos.x(), pos.y(), pos.z(), facing.toString(), width, height);
        ScreenManager.registerScreen(screen);
        screen.loadVideo(code);
    }

    KeyBinding testOpenMenu;
}
