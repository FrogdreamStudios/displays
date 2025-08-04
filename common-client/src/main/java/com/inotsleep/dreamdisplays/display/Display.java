package com.inotsleep.dreamdisplays.display;

import com.inotsleep.dreamdisplays.Config;
import com.inotsleep.dreamdisplays.media.AudioVideoPlayer;
import com.inotsleep.dreamdisplays.media.ytdlp.YouTubeCacheEntry;
import com.inotsleep.dreamdisplays.media.ytdlp.YtDlpExecutor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Display {
    AudioVideoPlayer player;

    private final UUID id;

    private final double x, y, z;
    private final int width, height;
    private final Facing facing;

    private int textureWidth, textureHeight;

    private int quality = Config.getInstance().defaultQuality;
    private float volume = (float) Config.getInstance().defaultVolume;
    private String language = "default";
    private String videoCode;

    private final ExecutorService executor;
    private final Thread safeUpdateDisplayThread;

    private volatile boolean throttled = false;

    public Display(UUID id, Facing facing, double x, double y, double z, int width, int height) {
        this.id = id;

        executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Display-" + id + " executor thread"));
        safeUpdateDisplayThread = new Thread(() -> {
            if (player.updateQualityAndLanguage()) afterInit();
        }, "Display-" + id + " updater thread");

        safeUpdateDisplayThread.setDaemon(true);

        this.facing = facing;

        this.x = x;
        this.y = y;
        this.z = z;

        this.width = width;
        this.height = height;

        player = new AudioVideoPlayer();
        safeUpdateDisplayThread.start();
        afterInit();
    }

    private void afterInit() {
        player.onInit(() -> {
            player.setVolume(volume);
            textureHeight = quality;
            textureWidth = (int) (((double) width)/height * quality);
            if (throttled) throttled = false;
        });
    }

    public void setQuality(int quality) {
        this.quality = quality;
        player.setQuality(quality);
    }

    public int getQuality() {
        return quality;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        player.setVolume(volume);
    }

    public float getVolume() {
        return volume;
    }

    public void setLanguage(String language) {
        this.language = language;
        player.setLanguage(language);
    }

    public String getLanguage() {
        return language;
    }

    public void setVideoCode(String videoCode) {
        this.videoCode = videoCode;
        player.setCode(videoCode);
    }

    public void startPlayer() {
        if (videoCode == null || videoCode.isEmpty()) return;

        executor.submit(() -> {
            player.initialize();
        });
    }

    public void mute(boolean mute) {
        player.mute(mute);
    }

    public void close() {
        safeUpdateDisplayThread.interrupt();
        executor.shutdown();
        player.close();
    }

    public void checkVALatency() {
        if (!Config.getInstance().reduceQualityOnHighLatency) return;
        if (!(player.getVideoAudioUsDifference() + Config.getInstance().maxVaLatency < 0)) return;
        if (throttled) return;

        executor.submit(() -> {
            YouTubeCacheEntry cacheEntry = YtDlpExecutor.getInstance().getFormats(videoCode);
            Optional<Integer> targetQuality = cacheEntry
                    .getVideoQualities()
                    .stream()
                    .filter(formatQuality -> formatQuality < quality)
                    .max(Integer::compareTo);

            if (targetQuality.isEmpty()) return;

            throttled = true;

            quality = targetQuality.get();
            player.setQuality(quality);
        });
    }

    public enum Facing {
        NORTH, EAST, SOUTH, WEST
    }
}
