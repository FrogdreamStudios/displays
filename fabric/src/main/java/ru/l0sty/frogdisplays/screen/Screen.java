package ru.l0sty.frogdisplays.screen;

import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import ru.l0sty.frogdisplays.PlatformlessInitializer;
import ru.l0sty.frogdisplays.net.DisplayInfoPacket;
import ru.l0sty.frogdisplays.net.RequestSyncPacket;
import ru.l0sty.frogdisplays.net.SyncPacket;
import ru.l0sty.frogdisplays.util.ImageUtil;
import ru.l0sty.frogdisplays.util.Utils;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.client.render.RenderPhase.ENABLE_LIGHTMAP;

/**
 * The Screen class represents a virtual screen object with multimedia functionality
 * for video playback, texture rendering, and positional management. It stores
 * information about its position, dimensions, facing direction, and video state.
 * This class also integrates synchronization with a remote server and allows
 * various operations such as video quality control, volume adjustment, and
 * media seeking.
 */
public class Screen {
    public static Thread safeQualitySwitchThread = new Thread(() -> {
        boolean isErrored = false;
        while (!isErrored) {
            ScreenManager.getScreens().forEach(Screen::reloadQuality);

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

    public boolean errored;

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
    private String quality = "480";
    public boolean isSync;
    public boolean muted;

    // Use a combined MediaPlayer instead of the separate VideoDecoder and AudioPlayer.
    private MediaPlayer mediaPlayer;

    private String videoUrl;

    public NativeImageBackedTexture texture = null;
    public Identifier textureId = null;
    public RenderLayer renderLayer = null;

    public int textureWidth = 0;
    public int textureHeight = 0;

    // Cache (good for performance)
    private transient BlockPos blockPos;

    private NativeImageBackedTexture previewTexture = null;
    public Identifier previewTextureId = null;
    public RenderLayer previewRenderLayer = null;
    private String lang;

    /**
     * Constructs a Screen object with the specified parameters.
     * @param id the unique identifier for the screen
     * @param ownerId the UUID of the owner of the screen
     * @param x the x-coordinate of the screen's position
     * @param y the y-coordinate of the screen's position
     * @param z the z-coordinate of the screen's position
     * @param facing the facing direction of the screen (e.g., "NORTH", "SOUTH", "EAST", "WEST")
     * @param width the width of the screen
     * @param height the height of the screen
     * @param isSync whether the screen is synchronized with the server
     */
    public Screen(UUID id, UUID ownerId, int x, int y, int z, String facing, int width, int height, boolean isSync) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.width = width;
        this.height = height;
        owner = MinecraftClient.getInstance().player != null && (ownerId + "").equals(MinecraftClient.getInstance().player.getUuid() + "");

        if (isSync) {
            sendRequestSyncPacket();
        }
    }

    /**
     * Loads a video into the screen with the specified URL and language.
     * @param videoUrl the URL of the video to load
     * @param lang the language of the video (e.g., "en", "ru")
     */
    public void loadVideo(String videoUrl, String lang) {
        LoggingManager.info("Loading video: " + videoUrl);

        if (mediaPlayer != null) unregister();

        // Load the video URL and language into the screen.
        this.videoUrl = videoUrl;
        this.lang = lang;
        CompletableFuture.runAsync(() -> {
            this.videoUrl = videoUrl;
            mediaPlayer = new MediaPlayer(videoUrl, lang, this);
            int qualityInt = Integer.parseInt(this.quality.replace("p", ""));
            textureWidth = (int) (width / (double) height * qualityInt);
            textureHeight = qualityInt;

            // TODO: note for INotSleep: we should delete video previews to avoid problems with videos
            ImageUtil.fetchImageTextureFromUrl("https://img.youtube.com/vi/" + Utils.extractVideoId(videoUrl) + "/maxresdefault.jpg")
                    .thenAcceptAsync(nativeImageBackedTexture -> {
                        previewTexture = nativeImageBackedTexture;
                        previewTextureId = Identifier.of(PlatformlessInitializer.MOD_ID, "screen-preview-"+id+"-"+UUID.randomUUID());

                        MinecraftClient.getInstance().getTextureManager().registerTexture(previewTextureId, previewTexture);
                        previewRenderLayer = createRenderLayer(previewTextureId);
                    });
        });

        waitForMFInit(this::startVideo);

        MinecraftClient.getInstance().execute(this::reloadTexture);
    }

    /**
     * Creates a RenderLayer for the screen texture.
     * @param id the Identifier for the texture
     * @return a RenderLayer for the screen texture
     */
    private static RenderLayer createRenderLayer(Identifier id) {
        return RenderLayer.of(
                "frog-displays",
                4194304,
                true,
                false,
                RenderPipelines.SOLID,
                RenderLayer.MultiPhaseParameters.builder().lightmap(ENABLE_LIGHTMAP).texture(new RenderPhase.Texture(id, TriState.FALSE, false)).build(true)
        );
    }

    /**
     * Updates the screen data based on a DisplayInfoPacket.
     * @param packet the DisplayInfoPacket containing the new data
     */
    public void updateData(DisplayInfoPacket packet) {
        this.x = packet.pos().x;
        this.y = packet.pos().y;
        this.z = packet.pos().z;

        this.facing = String.valueOf(packet.facing());

        this.width = packet.width();
        this.height = packet.height();
        this.isSync = packet.isSync();

        owner = MinecraftClient.getInstance().player != null && (packet.ownerId() + "").equals(MinecraftClient.getInstance().player.getUuid() + "");

        if (!Objects.equals(videoUrl, packet.url()) || !Objects.equals(lang, packet.lang())) {
            loadVideo(packet.url(), packet.lang());
            if (isSync) {
                sendRequestSyncPacket();
            }
        }
    }

    /**
     * Sends a request to synchronize the screen data with the server.
     */
    private void sendRequestSyncPacket() {
        PlatformlessInitializer.sendPacket(new RequestSyncPacket(id));
    }

    /**
     * Updates the screen data based on a SyncPacket.
     * @param packet the SyncPacket containing the new data
     */
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
            setPaused(packet.currentState());
        });
    }

    public void reloadTexture() {
        this.createTexture();
    }

    /**
     * Reloads the video quality by calling the MediaPlayer to set the new quality.
     */
    public void reloadQuality() {
        if (mediaPlayer != null) {
            mediaPlayer.setQuality(quality);
        }
    }

    /**
     * Checks if a given BlockPos is within the screen's area.
     * @param pos the BlockPos to check
     */
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

    /**
     * Checks if the video is started: if the MediaPlayer is not initialized, it will return false :(
     */
    public boolean isVideoStarted() {
        return mediaPlayer != null && mediaPlayer.textureFilled();
    }

    /**
     * Calculates the distance from a given BlockPos to the screen.
     * @param pos the BlockPos to calculate the distance to
     * @return the distance to the screen
     */
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
     * Fits the texture to the screen: if the MediaPlayer is not initialized, it will wait for it to be initialized and then update the frame.
     */
    public void fitTexture() {
        if (mediaPlayer != null) {
            mediaPlayer.updateFrame(texture.getGlTexture());
        }
    }

    /**
     * Returns the position of the screen as a BlockPos object.
     * @return the position of the screen
     */
    public BlockPos getPos() {
        if (blockPos == null) {
            blockPos = new BlockPos(x, y, z);
        }
        return blockPos;
    }

    /**
     * Returns the facing direction of the screen.
     * @return the facing direction of the screen
     */
    public String getFacing() {
        return facing;
    }

    /**
     * Returns the width and height of the screen.
     * @return the width and height of the screen
     */
    public float getWidth() {
        return width;
    }

    /**
     * We return the height of the screen.
     * @return the height of the screen
     */
    public float getHeight() {
        return height;
    }

    /**
     * Sets the volume of the video: if the MediaPlayer is not initialized, it will wait for it to be initialized and then set the volume.
     * @param volume the volume to set (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        this.volume = volume;
        setVideoVolume(volume);
    }

    /**
     * Sets the volume of the video: if the MediaPlayer is not initialized, it will wait for it to be initialized and then set the volume.
     * @param volume the volume to set (0.0 to 1.0)
     */
    public void setVideoVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    /**
     * Returns the quality of the video.
     * @return the quality of the video
     */
    public String getQuality() {
        return quality;
    }

    /**
     * Returns a list of available video qualities.
     * @return a list of available video qualities
     */
    public List<Integer> getQualityList() {
        if (mediaPlayer == null) return Collections.emptyList();
        return mediaPlayer.getAvailableQualities();
    }

    /**
     * Sets the quality of the video: if the MediaPlayer is not initialized, it will wait for it to be initialized and then set the quality.
     * @param quality the quality to set (e.g., "480", "720", "1080")
     */
    public void setQuality(String quality) {
        this.quality = quality;
    }

    /**
     * Starts the video: if the MediaPlayer is not initialized, it will wait for it to be initialized and then start the video.
     */
    public void startVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            videoStarted = true;
            paused = false;
        }
    }

    /**
     * Returns whether the video is paused.
     * @return true if the video is paused, false otherwise
     */
    public boolean getPaused() {
        return paused;
    }

    /**
     * Stop or play video: if the video is not started, it will start it, otherwise it will pause or resume it.
     * @param paused true to pause the video, false to resume it
     */
    public void setPaused(boolean paused) {
        if (!videoStarted) {
            this.paused = false;
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
        if (owner && isSync) sendSync();
    }

    // Relative seek video: moves the video by a specified number of seconds (in our case it's +5 seconds) relative to the current position
    public void seekForward() {
        seekVideoRelative(5);
    }

    //  Relative seek video: moves the video by a specified number of seconds (in our case it's -5 seconds) relative to the current position
    public void seekBackward() {
        seekVideoRelative(-5);
    }

    // Relative seek video: moves the video by a specified number of seconds relative to the current position
    public void seekVideoRelative(long seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seekRelative(seconds);
        }
    }

    // Absolute (cinema) seek video: moves to a specific second
    public void seekVideoTo(long nanos) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(nanos, false);
        }
    }

    public void unregister() {
        if (mediaPlayer != null) mediaPlayer.stop();

        TextureManager manager = MinecraftClient.getInstance().getTextureManager();

        if (textureId != null) manager.destroyTexture(textureId);
        if (previewTextureId != null) manager.destroyTexture(previewTextureId);

        if (MinecraftClient.getInstance().currentScreen instanceof DisplayConfScreen displayConfScreen) {
            if (displayConfScreen.screen == this) displayConfScreen.close();
        }
    }

    public NativeImageBackedTexture getPreviewTexture() {
        return previewTexture;
    }

    public boolean hasPreviewTexture() {
        return false;
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

    // Creates a new texture for the screen based on its dimensions and quality
    public void createTexture() {
        int qualityInt = Integer.parseInt(this.quality.replace("p", ""));
        textureWidth = (int) (width / (double) height * qualityInt);
        textureHeight = qualityInt;

        //textureId = RenderUtil2D.createEmptyTexture(textureWidth, textureHeight);
        if (texture != null) {
            texture.close();
            if (textureId != null) MinecraftClient.getInstance()
                    .getTextureManager()
                    .destroyTexture(textureId);
        }
        texture = new NativeImageBackedTexture(UUID.randomUUID().toString(), textureWidth, textureHeight, true);
        textureId = Identifier.of(PlatformlessInitializer.MOD_ID, "screen-main-texture-" + id + "-" + UUID.randomUUID());

        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
        renderLayer = createRenderLayer(textureId);
    }

    public void sendSync() {
        PlatformlessInitializer.sendPacket(new SyncPacket(id, isSync, paused, mediaPlayer.getCurrentTime(), mediaPlayer.getDuration()));
    }

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
        }).start();
    }

    public void tick(BlockPos pos) {
        if (mediaPlayer != null) mediaPlayer.tick(pos, PlatformlessInitializer.maxDistance);
    }

    public void afterSeek() {
        if (owner && isSync) sendSync();
    }
}