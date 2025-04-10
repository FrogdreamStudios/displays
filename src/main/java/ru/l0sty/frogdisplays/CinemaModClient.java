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
import ru.l0sty.frogdisplays.service.VideoServiceManager;
import net.fabricmc.api.ClientModInitializer;
import ru.l0sty.frogdisplays.testVideo.M3U8Links;
import ru.l0sty.frogdisplays.util.Facing;
import ru.l0sty.frogdisplays.util.RaycastUtil;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class CinemaModClient implements ClientModInitializer {

    public static boolean isOnScreen = false;
    private static CinemaModClient instance;
    public static String proxyAddress = "";

    public static double maxDistance = 64;
    public static boolean displaysEnabled = true;

    private Screen hoveredScreen = null;

    public static CinemaModClient getInstance() {
        return instance;
    }

    private VideoServiceManager videoServiceManager;
    private ScreenManager screenManager;
    private VideoSettings videoSettings;

    public VideoServiceManager getVideoServiceManager() {
        return videoServiceManager;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }

    public VideoSettings getVideoSettings() {
        return videoSettings;
    }

    @Override
    public void onInitializeClient() {


        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
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
            );
        });


        testOpenMenu = new KeyBinding(
                "Open menu to test", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, "key.categories.misc"
        );

        instance = this;

        PayloadTypeRegistry.playS2C().register(DisplaynInfoPacket.PACKET_ID, DisplaynInfoPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(DisplaySyncPacket.PACKET_ID, DisplaySyncPacket.PACKET_CODEC);

        PayloadTypeRegistry.playS2C().register(M3U8Packet.PACKET_ID, M3U8Packet.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(VideoInfoPacket.PACKET_ID, VideoInfoPacket.PACKET_CODEC);

        PayloadTypeRegistry.playC2S().register(M3U8RequestPacket.PACKET_ID, M3U8RequestPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(VideoInfoRequestPacket.PACKET_ID, VideoInfoRequestPacket.PACKET_CODEC);


        ClientPlayNetworking.registerGlobalReceiver(DisplaynInfoPacket.PACKET_ID, (payload, context) -> {
            createScreen(payload.id(), payload.pos(), payload.facing(), payload.width(), payload.height(), payload.url());
        });

        ClientPlayNetworking.registerGlobalReceiver(DisplaySyncPacket.PACKET_ID, (payload, context) -> {
            // TODO: handle DisplaySyncPacket
        });

        ClientPlayNetworking.registerGlobalReceiver(M3U8Packet.PACKET_ID, (payload, context) -> {
            CompletableFuture<M3U8Packet> future = M3U8Links.requestedM3U8Data.get(payload.url()+payload.quality());
            if (future != null) future.complete(payload);
        });

        ClientPlayNetworking.registerGlobalReceiver(VideoInfoPacket.PACKET_ID, (payload, context) -> {
            CompletableFuture<VideoInfoPacket> future = M3U8Links.requestedVideoInfo.get(payload.url());
            if (future != null) future.complete(payload);
        });

        ScreenWorldRenderer.register();

        videoServiceManager = new VideoServiceManager();
        screenManager = new ScreenManager();
        videoSettings = new VideoSettings();

        try {
            videoSettings.load();
        } catch (IOException e) {
            e.printStackTrace();
            CinemaMod.LOGGER.warning("Could not load video settings.");
        }

        new WindowFocusMuteThread().start();

        final boolean[] wasPressed = {false};
        AtomicBoolean wasInMultiplayer = new AtomicBoolean(false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.getCurrentServerEntry() != null) {
                wasInMultiplayer.set(true);
            } else {
                if (wasInMultiplayer.get()) {
                    wasInMultiplayer.set(false);
                    getScreenManager().unloadAll();
                    getVideoServiceManager().unregisterAll();
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

            for (Screen screen : CinemaModClient.getInstance().getScreenManager().getScreens()) {
                if (maxDistance < screen.getDistanceToScreen(client.player.getBlockPos()) || !displaysEnabled) getScreenManager().unregisterScreen(screen);

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
            getScreenManager().unloadAll();
            getVideoServiceManager().unregisterAll();
        });

    }

    private void checkAndOpenScreen() {
        if (hoveredScreen == null) return;
        DisplayConfScreen.open(hoveredScreen);
    }

    void createScreen(UUID id, Vector3i pos, Facing facing, int width, int height, String code) {
        var screen = new Screen(id, pos.x(), pos.y(), pos.z(), facing.toString(), width, height, true, false);
        screenManager.registerScreen(screen);
        screen.loadVideo(code);
//        var videoService = new VideoService("youtube", "https://cinemamod-static.ewr1.vultrobjects.com/service/v1/youtube.html",
//                "th_volume(%d);",
//                "th_video('%s', %b);",
//                "th_seek(%d);");
//        videoServiceManager.register(videoService);
//        var videoInfo = new VideoInfo(videoService, code);
//        var video = new Video(videoInfo, System.currentTimeMillis());
//        screen.loadVideo(video);
    }

    KeyBinding testOpenMenu;
}
