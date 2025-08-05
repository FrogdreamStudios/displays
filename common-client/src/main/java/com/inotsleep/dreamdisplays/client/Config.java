package com.inotsleep.dreamdisplays.client;

import me.inotsleep.utils.config.AbstractConfig;
import me.inotsleep.utils.config.Comment;
import me.inotsleep.utils.config.Path;

import java.io.File;

public class Config extends AbstractConfig {

    @Comment("Default quality")
    @Path("default_quality")
    public int defaultQuality = 720;

    @Comment("Default volume")
    @Path("default_volume")
    public double defaultVolume = 0.4;

    @Comment("Mod will not play sound when minecraft")
    @Comment("window is not focused")
    @Path("mute_on_lost_focus")
    public boolean muteOnLostFocus = true;

    @Comment("Mod will not render screen when minecraft")
    @Comment("window is not focused")
    @Path("no_render_on_lost_focus")
    public boolean noRenderOnLostFocus = true;

    @Comment("Quality will be reduced when")
    @Comment("latency between video and audio players")
    @Comment("exceeds max_va_latency value")
    @Path("reduce_quality_on_hight_va_latency")
    public boolean reduceQualityOnHighLatency = true;

    @Comment("Max latency between video and audio players")
    @Comment("Before quality will be reduced")
    @Comment("Specified in micro seconds")
    @Path("max_va_latency")
    public long maxVaLatency = 2000000;

    private static Config instance;

    public static Config getInstance() {
        return instance;
    }

    public Config(File baseDir) {
        super(baseDir, "config.yml");
    }
}
