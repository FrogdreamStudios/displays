package com.inotsleep.dreamdisplays.client.display;

import com.inotsleep.dreamdisplays.client.ClientModHolder;
import com.inotsleep.dreamdisplays.client.Config;
import com.inotsleep.dreamdisplays.client.DisplayManager;
import com.inotsleep.dreamdisplays.client.media.AudioVideoPlayer;
import com.inotsleep.dreamdisplays.client.media.ytdlp.YouTubeCacheEntry;
import com.inotsleep.dreamdisplays.client.media.ytdlp.YtDlpExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

public class Display {
    AudioVideoPlayer player;

    private final UUID id;
    private final UUID ownerId;

    private final double x, y, z;
    private final int width, height;
    private final Facing facing;

    private boolean sync = false;

    private List<Integer> availableQualities;
    private String savedQualitiesCode;

    private int textureWidth, textureHeight;

    private int quality = Config.getInstance().defaultQuality;
    private float volume = (float) Config.getInstance().defaultVolume;
    private String language = "default";
    private String videoCode;

    private final ExecutorService executor;
    private final Thread safeUpdateDisplayThread;

    private volatile boolean throttled = false;

    public Display(UUID id, UUID ownerId, Facing facing, double x, double y, double z, int width, int height) {
        this.id = id;
        this.ownerId = ownerId;

        executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Display-" + id + " executor thread"));
        safeUpdateDisplayThread = new Thread(() -> {
            if (player.updateQualityAndLanguage()) afterInit();
            LockSupport.parkNanos(1_000_000_000);
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

            if (availableQualities == null) {
                if (savedQualitiesCode != null && savedQualitiesCode.equals(videoCode)) return;

                YouTubeCacheEntry cacheEntry = YtDlpExecutor.getInstance().getFormats(videoCode);

                availableQualities = cacheEntry.getVideoQualities().stream().sorted().toList();
                savedQualitiesCode = videoCode;
            }
        });
    }

    public void setQuality(int quality) {
        this.quality = quality;
        player.setQuality(quality);

        DisplayManager.saveSettings(this);
    }

    public int getQuality() {
        return quality;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        player.setVolume(volume);

        DisplayManager.saveSettings(this);
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

    public DisplayRenderData getRenderData() {
        return new DisplayRenderData(player == null ? null : player.getImage(), x, y, z, width, height, facing, textureWidth, textureHeight);
    }

    public List<Integer> getAvailableQualities() {
        return availableQualities;
    }

    public UUID getId() {
        return id;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public void seekTo(long time) {
        player.seekTo(time);
    }

    public void seekForward() {
        if (sync && ClientModHolder.getInstance().getPlayerID().equals(id)) {
            ClientModHolder
                    .getInstance()
                    .sendSyncUpdate(
                            id,
                            Math.max(player.getPlayedAudioUs() + 5_000_000, player.getDuration()),
                            player.isPaused(),
                            sync,
                            player.getDuration()
                    );
        }

        player.seekRelative(5_000_000);
    }

    public void seekBackward() {
        if (sync && ClientModHolder.getInstance().getPlayerID().equals(id)) {
            ClientModHolder
                    .getInstance()
                    .sendSyncUpdate(
                            id,
                            Math.min(player.getPlayedAudioUs() - 5_000_000, 0),
                            player.isPaused(),
                            sync,
                            player.getDuration()
                    );
        }

        player.seekRelative(-5_000_000);
    }

    public boolean isPaused() {
        return player.isPaused();
    }

    public void setPaused(boolean paused) {
        player.setPaused(paused);
    }

    public enum Facing {
        NORTH, EAST, SOUTH, WEST;

        public static Facing fromPacket(byte data) {
            if (data < 0 || data >= values().length)
                throw new IllegalArgumentException("Invalid facing ID: " + data);
            return values()[data];
        }

        public byte toPacket() {
            return (byte) this.ordinal();
        }
    }
}
