package ru.l0sty.frogdisplays;

import it.unimi.dsi.fastutil.Hash;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import ru.l0sty.frogdisplays.block.PreviewScreenBlock;
import ru.l0sty.frogdisplays.block.PreviewScreenBlockEntity;
import ru.l0sty.frogdisplays.block.ScreenBlock;
import ru.l0sty.frogdisplays.block.ScreenBlockEntity;
import ru.l0sty.frogdisplays.block.render.PreviewScreenBlockEntityRenderer;
import ru.l0sty.frogdisplays.block.render.ScreenBlockEntityRenderer;
import ru.l0sty.frogdisplays.cef.CefUtil;
import ru.l0sty.frogdisplays.cef.Platform;
import ru.l0sty.frogdisplays.net.DisplayCreatePacket;
import ru.l0sty.frogdisplays.screen.PreviewScreenManager;
import ru.l0sty.frogdisplays.screen.Screen;
import ru.l0sty.frogdisplays.screen.ScreenManager;
import ru.l0sty.frogdisplays.service.VideoService;
import ru.l0sty.frogdisplays.service.VideoServiceManager;
import net.fabricmc.api.ClientModInitializer;
import ru.l0sty.frogdisplays.video.Video;
import ru.l0sty.frogdisplays.video.VideoInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CinemaModClient implements ClientModInitializer {

    private static CinemaModClient instance;

    public static CinemaModClient getInstance() {
        return instance;
    }

    private VideoServiceManager videoServiceManager;
    private ScreenManager screenManager;
    private PreviewScreenManager previewScreenManager;
    private VideoSettings videoSettings;

    public VideoServiceManager getVideoServiceManager() {
        return videoServiceManager;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }

    public PreviewScreenManager getPreviewScreenManager() {
        return previewScreenManager;
    }

    public VideoSettings getVideoSettings() {
        return videoSettings;
    }


    private static void initCefMac() {
        // TODO: fixme
        if (Platform.getPlatform().isMacOS()) {
            Util.getMainWorkerExecutor().execute(() -> {
                if (CefUtil.init()) {
                    CinemaMod.LOGGER.info("Chromium Embedded Framework initialized for macOS");
                } else {
                    CinemaMod.LOGGER.warning("Could not initialize Chromium Embedded Framework for macOS");
                }
            });
        }
    }

    @Override
    public void onInitializeClient() {
        instance = this;

        // Hack for initializing CEF on macos
        initCefMac();

        // Register ScreenBlock
        ScreenBlock.register();
        ScreenBlockEntity.register();
        ScreenBlockEntityRenderer.register();

        // Register PreviewScreenBlock
        PreviewScreenBlock.register();
        PreviewScreenBlockEntity.register();
        PreviewScreenBlockEntityRenderer.register();

        //NetworkUtil.registerReceivers();

        videoServiceManager = new VideoServiceManager();
        screenManager = new ScreenManager();
        previewScreenManager = new PreviewScreenManager();
        videoSettings = new VideoSettings();

        try {
            videoSettings.load();
        } catch (IOException e) {
            e.printStackTrace();
            CinemaMod.LOGGER.warning("Could not load video settings.");
        }

        new WindowFocusMuteThread().start();
        ClientTickEvents.START_CLIENT_TICK.register(client -> onTick());
        CREATE_KEYMAPPING = new KeyBinding(
                "Open Basic Browser", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, "key.categories.misc"
        );
        START_KEYMAPPING = new KeyBinding(
                "Start Video", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, "key.categories.misc"
        );


//        PayloadTypeRegistry.playS2C().register(DisplayCreatePacket.PACKET_ID, DisplayCreatePacket.PACKET_CODEC);
//        ClientPlayNetworking.registerGlobalReceiver(DisplayCreatePacket.PACKET_ID, (packet, context) -> {
//            context.client().executeTask(() -> {
//                createScreen(packet.pos());
//            });
//        });


    }

    public static boolean toggle = false;
    private Map<Screen, Boolean> screens = new ConcurrentHashMap<>();
    void createScreen(Vector3i pos) {
        var screen = new Screen(pos.x(), pos.y(), pos.z(), "NORTH", 16, 9, true, false);
        screens.put(screen, false);
        screenManager.registerScreen(screen);
        var videoService = new VideoService("youtube", "https://cinemamod-static.ewr1.vultrobjects.com/service/v1/youtube.html",
                "th_volume(%d);",
                "th_video('%s', %b);",
                "th_seek(%d);");
        videoServiceManager.register(videoService);
        var videoInfo = new VideoInfo(videoService, "dQw4w9WgXcQ");
        var video = new Video(videoInfo, System.currentTimeMillis());
        screen.loadVideo(video);
    }
    private void onTick() {
        if (CREATE_KEYMAPPING.wasPressed()) {
            System.out.println("Key pressed");

            if (!toggle) {
                CefUtil.init();
                toggle = true;
            }
            createScreen(MinecraftClient.getInstance().crosshairTarget.getPos().toVector3f().get(RoundingMode.FLOOR, new Vector3i()));


            //screen.startVideo();
        }
        if (START_KEYMAPPING.wasPressed()) {
            screens.forEach((screen, aBoolean) -> {
                if (!aBoolean) {
                    screen.startVideo();
                    System.out.println("Screen video start");
                } else {
                    //screen.setVideoVolume(0.2f);
                    //screen.seekVideo(2332);
                }
                screens.put(screen, true);
            });
        }
    }

    private KeyBinding CREATE_KEYMAPPING;
    private KeyBinding START_KEYMAPPING;

}
