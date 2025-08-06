package com.inotsleep.dreamdisplays.client;

import me.inotsleep.utils.config.AbstractConfig;
import me.inotsleep.utils.config.Comment;
import me.inotsleep.utils.config.Path;

import java.io.File;

public class Config extends AbstractConfig {

    @Comment("Render distance for displays")
    @Path("render_distance")
    public int renderDistance = 64;

    @Comment("Default quality")
    @Path("default-quality")
    public int defaultQuality = 720;

    @Comment("Default volume")
    @Path("default-volume")
    public double defaultVolume = 0.4;

    @Comment("Mod will not play sound when minecraft")
    @Comment("window is not focused")
    @Path("mute-on-lost-focus")
    public boolean muteOnLostFocus = true;

    @Comment("Mod will not render screen when minecraft")
    @Comment("window is not focused")
    @Path("no-render-on-lost-focus")
    public boolean noRenderOnLostFocus = true;

    @Comment("Quality will be reduced when")
    @Comment("latency between video and audio players")
    @Comment("exceeds max-va-latency value")
    @Path("reduce-quality-on-hight-va-latency")
    public boolean reduceQualityOnHighLatency = true;

    @Comment("Max latency between video and audio players")
    @Comment("Before quality will be reduced")
    @Comment("Specified in micro seconds")
    @Path("max-va-latency")
    public long maxVaLatency = 2000000;

    @Comment("Disable displays")
    @Path("displays-disabled")
    public boolean displaysDisabled = false;


    private static Config instance;

    public static Config getInstance() {
        return instance;
    }

    public Config(File baseDir) {
        super(baseDir, "config.yml");
        instance = this;
    }
}
