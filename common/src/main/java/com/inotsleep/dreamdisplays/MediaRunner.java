package com.inotsleep.dreamdisplays;

import com.inotsleep.dreamdisplays.media.AudioVideoPlayer;
import com.inotsleep.dreamdisplays.media.ytdlp.YtDlpExecutor;

public class MediaRunner {
    public static void run() throws Exception {
        AudioVideoPlayer player = new AudioVideoPlayer("3UCI4cUFlVs");

        Thread volumeOscillator = new Thread(() -> {
            final double periodMs = 5000.0;
            final double maxVol = 0.6;
            final double minVol = 0.3;
            long start = System.currentTimeMillis();
            try {
                while (true) {
                    long now = System.currentTimeMillis();
                    double t = (now - start) % periodMs;
                    double sine = Math.sin(2 * Math.PI * t / periodMs);
                    double norm = (sine + 1) / 2;
                    float vol = (float) (minVol + (maxVol - minVol) * norm);

                    player.setVolume(vol);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "VolumeOscillator");

        volumeOscillator.setDaemon(true);
        volumeOscillator.start();

        player.initialize(YtDlpExecutor.getInstance());
    }
}
