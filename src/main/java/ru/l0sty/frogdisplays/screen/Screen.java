package ru.l0sty.frogdisplays.screen;

import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.texture.NativeImageBackedTexture;
import ru.l0sty.frogdisplays.buffer.DisplaysCustomPayload;
import ru.l0sty.frogdisplays.cef.CefUtil;
import ru.l0sty.frogdisplays.util.ImageUtil;
import ru.l0sty.frogdisplays.video.Video;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Screen extends DisplaysCustomPayload<Screen> {

    private int x;
    private int y;
    private int z;
    private String facing;
    private int width;
    private int height;
    private boolean visible;
    private boolean muted;

    private transient MCEFBrowser browser;
    private transient Video video;
    private transient boolean unregistered;
    private transient BlockPos blockPos; // used as a cache for performance

    public Screen(int x, int y, int z, String facing, int width, int height, boolean visible, boolean muted) {
        this();
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.width = width;
        this.height = height;
        this.visible = visible;
        this.muted = muted;
    }

    public boolean isInScreen(BlockPos pos) {
        int maxX = x;
        int maxY = y+height-1;
        int maxZ = z;

        switch (facing) {
            case "NORTH", "SOUTH" -> {
                maxX += width-1;
            }
            default -> {
                maxZ += width-1;
            }
        }

        return x <= pos.getX() && maxX >= pos.getX() &&
                y <= pos.getY() && maxY >= pos.getY() &&
                z <= pos.getZ() && maxZ >= pos.getZ();
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

    public void reload() {
        if (video != null) {
            loadVideo(video);
        }
    }

    public void loadVideo(Video video) {
        this.video = video;

        ImageUtil.fetchImageTextureFromUrl(video.getVideoInfo().getThumbnailUrl()).thenAccept((nativeImageBackedTexture ->
                texture = nativeImageBackedTexture
        ));

        closeBrowser();
        browser = CefUtil.createBrowser(video.getVideoInfo().getVideoService().getUrl(), this);
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

    public void setVideoVolume(float volume) {
        if (browser != null && video != null) {
            String js = video.getVideoInfo().getVideoService().getSetVolumeJs();

            // 0-100 volume
            if (js.contains("%d")) {
                js = String.format(js, (int) (volume * 100));
            }

            // 0.00-1.00 volume
            else if (js.contains("%f")) {
                js = String.format(js, volume);
            }

            browser.getMainFrame().executeJavaScript(js, browser.getURL(), 0);
        }
    }

    public void startVideo() {
        if (browser != null && video != null) {
            String startJs = video.getVideoInfo().getVideoService().getStartJs();

            if (startJs.contains("%s") && startJs.contains("%b")) {
                startJs = String.format(startJs, video.getVideoInfo().getId(), video.getVideoInfo().isLivestream());
            } else if (startJs.contains("%s")) {
                startJs = String.format(startJs, video.getVideoInfo().getId());
            }

            browser.getMainFrame().executeJavaScript(startJs, browser.getURL(), 0);

            // Seek to current time
            if (!video.getVideoInfo().isLivestream()) {
                long millisSinceStart = System.currentTimeMillis() - video.getStartedAt();
                long secondsSinceStart = millisSinceStart / 1000;
                if (secondsSinceStart < video.getVideoInfo().getDurationSeconds()) {
                    String seekJs = video.getVideoInfo().getVideoService().getSeekJs();

                    if (seekJs.contains("%d")) {
                        seekJs = String.format(seekJs, secondsSinceStart);
                    }

                    browser.getMainFrame().executeJavaScript(seekJs, browser.getURL(), 0);
                }
            }
        }
    }

    public void waitForMFInit(Runnable action) {
        Thread waitForBrowserInitThread = new Thread(() -> {
            boolean isInit = false;
            while (!isInit) {
                if (getBrowser().getMainFrame() != null) {
                    isInit = true;
                    try {
                        Thread.sleep(300);
                        action.run();

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        waitForBrowserInitThread.start();
    }

    public void seekVideo(int seconds) {
        if (browser != null && video != null) {
            String js = video.getVideoInfo().getVideoService().getSeekJs();

            // 0-100 volume
            if (js.contains("%d")) {
                js = String.format(js, seconds);
            }

            browser.getMainFrame().executeJavaScript(js, browser.getURL(), 0);
        }
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void unregister() {
        unregistered = true;
    }

    NativeImageBackedTexture texture = null;

    public NativeImageBackedTexture getPreviewTexture() {
        return texture;
    }

    public boolean hasPreviewTexture() {
        return texture != null;
    }
}
