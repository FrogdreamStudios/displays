package ru.l0sty.frogdisplays.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import ru.l0sty.frogdisplays.net.DisplayInfoPacket;
import ru.l0sty.frogdisplays.net.SyncPacket;
import ru.l0sty.frogdisplays.render.RenderUtil2D;
import ru.l0sty.frogdisplays.util.ImageUtil;
import ru.l0sty.frogdisplays.util.Utils;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Screen {

    public static Thread safeQualitySwitchThread = new Thread(() -> {
        boolean isErrored = false;
        while (!isErrored) {
            ScreenManager.getScreens().forEach(screen -> {
                 screen.reloadQuality();
            });

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                isErrored = true;
            }
        }
    });

    static {
        safeQualitySwitchThread.start();
    }

    public boolean owner;

    private int x;
    private int y;
    private int z;
    private String facing;
    private int width;
    private int height;
    private final UUID id;
    private float volume;
    private boolean videoStarted;
    private boolean paused;
    private String quality = "144";
    public boolean isSync;
    public boolean muted;

    // Используем объединённый MediaPlayer вместо отдельных VideoDecoder и AudioPlayer.
    private MediaPlayer mediaPlayer;

    private String videoUrl;
    public int textureId = -1;
    public int removalTextureId = -1;

    public int textureWidth = 0;
    public int textureHeight = 0;

    private transient BlockPos blockPos; // кэш позиции для производительности

    private NativeImageBackedTexture previewTexture = null;

    public Screen(UUID id, UUID ownerId, int x, int y, int z, String facing, int width, int height, boolean isSync) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.width = width;
        this.height = height;
        owner = MinecraftClient.getInstance().player != null && ownerId == MinecraftClient.getInstance().player.getUuid();
    }

    public void loadVideo(String videoUrl) {
        System.out.println("Loading video: " + videoUrl);

        if (mediaPlayer != null) unregister();
        // Загружаем превью-изображение из YouTube (используем максимальное разрешение)
        CompletableFuture.runAsync(() -> {
            this.videoUrl = videoUrl;
            mediaPlayer = new MediaPlayer(videoUrl, this);
            int qualityInt = Integer.parseInt(this.quality.replace("p", ""));
            textureWidth = (int) (width / (double) height * qualityInt);
            textureHeight = qualityInt;
            ImageUtil.fetchImageTextureFromUrl("https://img.youtube.com/vi/" + Utils.extractVideoId(videoUrl) + "/maxresdefault.jpg")
                    .thenAcceptAsync(nativeImageBackedTexture -> previewTexture = nativeImageBackedTexture);
        });

        reloadTexture();
    }

    public void updateData(DisplayInfoPacket packet) {
        this.x = packet.pos().x;
        this.y = packet.pos().y;
        this.z = packet.pos().z;

        this.facing = String.valueOf(packet.facing());

        this.width = packet.width();
        this.height = packet.height();
        this.isSync = packet.isSync();

        owner = MinecraftClient.getInstance().player != null && packet.ownerId() == MinecraftClient.getInstance().player.getUuid();

        if (!Objects.equals(videoUrl, packet.url())) loadVideo(packet.url());
    }

    public void updateData(SyncPacket packet) {
        isSync = packet.isSync();
        if (!isSync) return;

        long nanos = System.nanoTime();

        waitForMFInit(() -> {
            if (!videoStarted) {
                startVideo();
                setVolume(0);
            }

            if (paused) setPaused(false);

            long lostTime = System.nanoTime() - nanos;

            seekVideoTo(packet.currentTime() + lostTime);
        });
    }

    private void reloadTexture() {
        textureId = -1;
    }

    /**
     * Перезагружает качество видео, вызывая у MediaPlayer установку нового качества.
     */
    private void reloadQuality() {
        if (mediaPlayer != null) {
            mediaPlayer.setQuality(quality);
            reloadTexture();
        }
    }

    public boolean isVideoStarted() {
        return videoStarted && mediaPlayer != null && mediaPlayer.textureFilled();
    }

    public boolean isInScreen(BlockPos pos) {
        int maxX = x;
        int maxY = y + height - 1;
        int maxZ = z;

        switch (facing) {
            case "NORTH", "SOUTH" -> maxX += width - 1;
            default -> maxZ += width - 1;
        }

        return x <= pos.getX() && maxX >= pos.getX() &&
                y <= pos.getY() && maxY >= pos.getY() &&
                z <= pos.getZ() && maxZ >= pos.getZ();
    }

    public double getDistanceToScreen(BlockPos pos) {
        int maxX = x;
        int maxY = y + height - 1;
        int maxZ = z;

        switch (facing) {
            case "NORTH", "SOUTH" -> maxX += width - 1;
            case "EAST", "WEST" -> maxZ += width - 1;
        }

        int clampedX = Math.min(Math.max(pos.getX(), x), maxX);
        int clampedY = Math.min(Math.max(pos.getY(), y), maxY);
        int clampedZ = Math.min(Math.max(pos.getZ(), z), maxZ);

        BlockPos closestPoint = new BlockPos(clampedX, clampedY, clampedZ);

        return Math.sqrt(pos.getSquaredDistance(closestPoint));
    }

    /**
     * Вызывает обновление текстуры текущим кадром видео.
     */
    public void fitTexture() {
        if (mediaPlayer != null) {
            mediaPlayer.updateFrame(textureId);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public BlockPos getPos() {
        if (blockPos == null) {
            blockPos = new BlockPos(x, y, z);
        }
        return blockPos;
    }

    public String getFacing() {
        return facing;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    /**
     * Устанавливает громкость для MediaPlayer.
     */
    public void setVolume(float volume) {
        this.volume = volume;
        setVideoVolume(volume);
    }

    public void setVideoVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public String getQuality() {
        return quality;
    }

    public List<Integer> getQualityList() {
        if (mediaPlayer == null) return Collections.emptyList();
        return mediaPlayer.getAvailableQualities();
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    /**
     * Запускает воспроизведение видео и аудио через MediaPlayer.
     */
    public void startVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            videoStarted = true;
            paused = false;
        }
    }

    public boolean getPaused() {
        return paused;
    }

    /**
     * Приостанавливает/возобновляет воспроизведение через MediaPlayer.
     */
    public void setPaused(boolean paused) {
        if (!videoStarted) {
            waitForMFInit(() -> {
                startVideo();
                setVolume(0);
            });
            return;
        }
        this.paused = paused;
        if (mediaPlayer != null) {
            if (paused) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        }
    }

    /**
     * Перематывает видео на 5 секунд вперёд (относительный seek).
     */
    public void seekForward() {
        seekVideoRelative(5);
    }

    /**
     * Перематывает видео на 5 секунд назад (относительный seek).
     */
    public void seekBackward() {
        seekVideoRelative(-5);
    }

    /**
     * Относительный seek видео: перемещает видео на заданное число секунд относительно текущей позиции.
     *
     * @param seconds число секунд для сдвига (может быть отрицательным)
     */
    public void seekVideoRelative(long seconds) {
        System.out.println("Seeking " + seconds);

        if (mediaPlayer != null) {
            mediaPlayer.seekRelative(seconds);
        }
    }

    /**
     * Абсолютный seek видео: переходит к конкретной секунде.
     *
     * @param nanos время в наносекундах, к которому нужно перейти
     */
    public void seekVideoTo(long nanos) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(nanos);
        }
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void unregister() {
        if (mediaPlayer != null) mediaPlayer.stop();
    }

    public NativeImageBackedTexture getPreviewTexture() {
        return previewTexture;
    }

    public boolean hasPreviewTexture() {
        return previewTexture != null;
    }

    public UUID getID() {
        return id;
    }

    public void mute(boolean b) {
        if (muted == b) return;
        muted = b;

        setVideoVolume(!b ? volume : 0);
    }

    public double getVolume() {
        return volume;
    }

    /**
     * Создаёт текстуру для отображения видео с учётом текущего качества.
     */
    public void createTexture() {
        int qualityInt = Integer.parseInt(this.quality.replace("p", ""));
        textureWidth = (int) (width / (double) height * qualityInt);
        textureHeight = qualityInt;

        textureId = RenderUtil2D.createEmptyTexture(textureWidth, textureHeight);
    }

    public void sendSync() {
        ClientPlayNetworking.send(new SyncPacket(id, isSync, paused, mediaPlayer.getCurrentTime(), mediaPlayer.getDuration()));
    }

    /**
     * Метод для ожидания инициализации MediaPlayer (например, первого кадра)
     * и выполнения заданного действия.
     */
    public void waitForMFInit(Runnable action) {
        new Thread(() -> {
            while (mediaPlayer == null || !mediaPlayer.isInitialized()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            action.run();
            System.out.println("Started action!");
        }).start();
    }
}
