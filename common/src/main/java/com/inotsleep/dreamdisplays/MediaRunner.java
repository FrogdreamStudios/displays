package com.inotsleep.dreamdisplays;

import com.inotsleep.dreamdisplays.media.AudioVideoPlayer;
import me.inotsleep.utils.logging.LoggingManager;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class MediaRunner {

    private static final int TARGET_FPS = 60;
    private static final long RENDER_INTERVAL_NS = 1_000_000_000L / TARGET_FPS;
    private static final long SPIN_NS = 200_000; // 0.2 ms
    private static final ColorSpace SRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    private static final long LEAD_US = 200_000; // ограничение опережения при выводе

    public static void run() {
        AudioVideoPlayer player = new AudioVideoPlayer("glQjLPFd2M8", 720, "default");
        ImageWindow window = new ImageWindow("Video display", 640, 480);
        AtomicBoolean running = new AtomicBoolean(true);
        Java2DFrameConverter converter = new Java2DFrameConverter();

        player.onInit(() -> {
            try {
                player.setVolume(0.5f);
                player.setPaused(false);
            } catch (Exception e) {
                LoggingManager.error("Init task error", e);
            }

            Thread renderThread = new Thread(() -> {
                long next = System.nanoTime();
                BufferedImage lastImg = null;

                while (running.get()) {
                    long now = System.nanoTime();
                    if (now < next) {
                        LockSupport.parkNanos(next - now);
                        continue;
                    }
                    next += RENDER_INTERVAL_NS;

                    BufferedImage image = player.getImage();
                    if (image != null) {
                        window.setImage(image);
                    } else {
                        LockSupport.parkNanos(SPIN_NS);
                    }
                }
            }, "RenderThread");

            renderThread.setDaemon(true);
            renderThread.start();
        });

        player.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            try {
                player.close();
                converter.close();
            } catch (Exception ignored) {}
        }));
    }
}