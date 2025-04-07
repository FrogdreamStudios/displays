package ru.l0sty.frogdisplays.screen;

import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.texture.NativeImageBackedTexture;
import ru.l0sty.frogdisplays.buffer.DisplaysCustomPayload;
import ru.l0sty.frogdisplays.render.RenderUtil2D;
import ru.l0sty.frogdisplays.testVideo.M3U8Links;
import ru.l0sty.frogdisplays.testVideo.MediaPlayer;
import ru.l0sty.frogdisplays.util.ImageUtil;
import ru.l0sty.frogdisplays.util.Utils;
import ru.l0sty.frogdisplays.video.Video;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Screen extends DisplaysCustomPayload<Screen> {
    private int x;
    private int y;
    private int z;
    private String facing;
    private int width;
    private int height;
    private boolean visible;
    private boolean muted;
    private UUID id;
    private float volume;
    private boolean videoStarted;
    private boolean paused;
    private String quality = "720p";

    List<String> qualities;

    // Используем объединённый MediaPlayer вместо отдельных VideoDecoder и AudioPlayer.
    private MediaPlayer mediaPlayer;

    private String videoUrl;
    public int textureId = -1;
    public int removalTextureId = -1;

    private int textureWidth = -1;
    private int textureHeight = -1;

    private transient MCEFBrowser browser;
    private transient Video video;
    private transient boolean unregistered;
    private transient BlockPos blockPos; // кэш позиции для производительности

    private long playTime = 0;
    private long startTime = 0;
    private int duration;

    public Screen(UUID id, int x, int y, int z, String facing, int width, int height, boolean visible, boolean muted) {
        this();
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.width = width;
        this.height = height;
        this.visible = visible;
        this.muted = muted;
    }

    /**
     * Загружает видео по заданному URL.
     * Предварительно подгружается превью и информация о доступных качествах,
     * а затем создаётся экземпляр MediaPlayer на основе m3u8-ссылок.
     */
    public void loadVideo(String videoUrl) {
        ImageUtil.fetchImageTextureFromUrl("https://img.youtube.com/vi/" + Utils.extractVideoId(videoUrl) + "/maxresdefault.jpg")
                .thenAccept(nativeImageBackedTexture -> previewTexture = nativeImageBackedTexture);
        this.videoUrl = videoUrl;

        M3U8Links.getVideoInfo(videoUrl).thenAcceptAsync(videoInfoPacket -> {
            qualities = new ArrayList<>(videoInfoPacket.qualities());
            duration = videoInfoPacket.duration();
        });

        M3U8Links.getM3U8(videoUrl, quality).thenAcceptAsync(m3u8LinksPacket -> {
            mediaPlayer = new MediaPlayer(m3u8LinksPacket.videoUrl(), m3u8LinksPacket.audioUrl());
        });

        reloadTexture();
    }

    private void reloadTexture() {
        if (textureId != -1) {
            removalTextureId = textureId;
        }
        textureId = -1;
    }

    /**
     * Перезагружает качество видео, обновляя m3u8-ссылки для MediaPlayer.
     */
    private void reloadQuality() {
        M3U8Links.getM3U8(videoUrl, quality).thenAcceptAsync(m3u8LinksPacket -> {
            double videoT = mediaPlayer.getVideoCurrentTime();

            mediaPlayer.close();
            mediaPlayer = new MediaPlayer(m3u8LinksPacket.videoUrl(), m3u8LinksPacket.audioUrl());
            mediaPlayer.play();

            mediaPlayer.seekTo(videoT);
        });
    }

    public boolean isVideoStarted() {
        return videoStarted;
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
            mediaPlayer.updateFrame(textureId, textureWidth, textureHeight);
        }
    }

    public Screen() {
        super("screens");
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

    public boolean isVisible() {
        return visible;
    }

    public boolean isMuted() {
        return muted;
    }

    public MCEFBrowser getBrowser() {
        return browser;
    }

    public boolean hasBrowser() {
        return browser != null;
    }

    public long getPlayTime() {
        return playTime;
    }

    public void reload() {
        if (video != null) {
            playTime = (System.currentTimeMillis() - startTime);
            loadVideo(video);
        }
    }

    public void loadVideo(Video video) {
        this.video = video;
        ImageUtil.fetchImageTextureFromUrl(video.getVideoInfo().getThumbnailUrl())
                .thenAccept(nativeImageBackedTexture -> previewTexture = nativeImageBackedTexture);
        // Дополнительная логика работы с браузером может быть здесь (закомментировано)
    }

    public void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }

    public Video getVideo() {
        return video;
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

    public void setQuality(String quality) {
        this.quality = quality;
        reloadQuality();
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
        this.paused = paused;
        if (mediaPlayer != null) {
            if (paused) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.resume();
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
        if (mediaPlayer != null) {
            mediaPlayer.seekRelative(seconds);
        }
    }

    /**
     * Абсолютный seek видео: переходит к конкретной секунде.
     *
     * @param seconds время в секундах, к которому нужно перейти
     */
    public void seekVideoTo(long seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(seconds);
        }
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void unregister() {
        unregistered = true;
        if (mediaPlayer != null) mediaPlayer.close();
    }

    private NativeImageBackedTexture previewTexture = null;

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
        setVideoVolume(b ? volume : 0);
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

    /**
     * Метод для ожидания инициализации MediaPlayer (например, первого кадра)
     * и выполнения заданного действия.
     */
    public void waitForMFInit(Runnable action) {
        new Thread(() -> {
            while (mediaPlayer == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            action.run();
        }).start();
    }
}