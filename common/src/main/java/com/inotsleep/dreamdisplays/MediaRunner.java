package com.inotsleep.dreamdisplays;

import com.inotsleep.dreamdisplays.media.AudioVideoPlayer;
import com.inotsleep.dreamdisplays.media.ytdlp.YtDlpExecutor;
import org.bytedeco.javacv.Frame;

public class MediaRunner {
    public static void run() throws Exception {
        AudioVideoPlayer player = new AudioVideoPlayer("3UCI4cUFlVs");

        ImageWindow window = new ImageWindow("Video display", 640, 480);

        player.onInit(() -> {
            player.setVolume(0.5f);

            Thread renderThread = new Thread(() -> {
                while (true) {
                    Frame frame = player.getFrame();
                    if (frame != null) window.setFrame(frame);
                    try {
                        Thread.sleep(1000/60);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            renderThread.setDaemon(true);
            renderThread.start();

            player.setPaused(false);
        });

        player.initialize(YtDlpExecutor.getInstance());
    }
}
