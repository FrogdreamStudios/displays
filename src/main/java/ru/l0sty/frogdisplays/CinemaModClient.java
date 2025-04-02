package ru.l0sty.frogdisplays;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import ru.l0sty.frogdisplays.render.ScreenWorldRenderer;
import ru.l0sty.frogdisplays.cef.CefUtil;
import ru.l0sty.frogdisplays.cef.Platform;
import ru.l0sty.frogdisplays.net.DisplaynInfoPacket;
import ru.l0sty.frogdisplays.screen.Screen;
import ru.l0sty.frogdisplays.screen.ScreenManager;
import ru.l0sty.frogdisplays.service.VideoService;
import ru.l0sty.frogdisplays.service.VideoServiceManager;
import net.fabricmc.api.ClientModInitializer;
import ru.l0sty.frogdisplays.util.Facing;
import ru.l0sty.frogdisplays.util.RaycastUtil;
import ru.l0sty.frogdisplays.video.Video;
import ru.l0sty.frogdisplays.video.VideoInfo;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CinemaModClient implements ClientModInitializer {

    public static boolean isOnScreen = false;
    private static CinemaModClient instance;
    public static String proxyAddress = "";

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

        PayloadTypeRegistry.playS2C().register(DisplaynInfoPacket.PACKET_ID, DisplaynInfoPacket.PACKET_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DisplaynInfoPacket.PACKET_ID, (payload, context) -> {
            createScreen(payload.pos(), payload.facing(), payload.width(), payload.height(), payload.url());
        });

        ScreenWorldRenderer.register();

        // Hack for initializing CEF on macos
        initCefMac();

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

        CREATE_KEYMAPPING = new KeyBinding(
                "Open Basic Browser", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, "key.categories.misc"
        );
        START_KEYMAPPING = new KeyBinding(
                "Start Video", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, "key.categories.misc"
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BlockHitResult result = RaycastUtil.raycastBlockClient(32);
            if (result == null) {
                isOnScreen = false;
                return;
            }
            BlockPos pos = result.getBlockPos();

            for (Screen screen : CinemaModClient.getInstance().getScreenManager().getScreens()) {
                if (screen.isInScreen(pos)) {
                    isOnScreen = true;
                    return;
                }
            }

            isOnScreen = false;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RenderSystem.recordRenderCall(() -> {
                getScreenManager().unloadAll();
                getVideoServiceManager().unregisterAll();
            });
            toggle = false;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            getScreenManager().unloadAll();
            getVideoServiceManager().unregisterAll();
        });

    }

    public static boolean toggle = false;
    private Map<Screen, Boolean> screens = new ConcurrentHashMap<>();
    void createScreen(Vector3i pos, Facing facing, int width, int height, String code) {
        if (!toggle) {
            CefUtil.init();
            toggle = true;
        }

        var screen = new Screen(pos.x(), pos.y(), pos.z(), facing.toString(), width, height, true, false);
        screens.put(screen, false);
        screenManager.registerScreen(screen);
        var videoService = new VideoService("youtube", "https://cinemamod-static.ewr1.vultrobjects.com/service/v1/youtube.html",
                "th_volume(%d);",
                "th_video('%s', %b);",
                "th_seek(%d);");
        videoServiceManager.register(videoService);
        var videoInfo = new VideoInfo(videoService, code);
        var video = new Video(videoInfo, System.currentTimeMillis());
        screen.loadVideo(video);
        screen.waitForMFInit(screen::startVideo);
    }

    private KeyBinding CREATE_KEYMAPPING;
    private KeyBinding START_KEYMAPPING;

}
