package com.inotsleep.dreamdisplays;

import com.inotsleep.dreamdisplays.media.AudioVideoPlayer;
import com.inotsleep.dreamdisplays.media.ytdlp.YtDlpExecutor;
import org.bytedeco.javacv.Frame;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaRunner {
    public static void run() throws Exception {
        AudioVideoPlayer player = new AudioVideoPlayer("3UCI4cUFlVs", 144, "default");

        ImageWindow window = new ImageWindow("Video display", 640, 480);

        player.onInit(() -> {
            player.setVolume(0.5f);
            AtomicInteger frameIndex = new AtomicInteger();
            Thread renderThread = new Thread(() -> {

                while (true) {
                    frameIndex.getAndIncrement();
                    BufferedImage image = player.getImage();
                    if (image != null) window.setImage(image);
                    try {
                        Thread.sleep(1000/60);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            });

            renderThread.setDaemon(true);
            renderThread.start();

            player.setPaused(false);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }

            player.setQuality(480);
            player.setCode("glQjLPFd2M8");
            player.updateQualityAndLanguage();
        });

        player.initialize();
    }
}
