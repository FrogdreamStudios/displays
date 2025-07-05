package ru.l0sty.dreamdisplays;

import me.inotsleep.utils.config.AbstractConfig;
import me.inotsleep.utils.config.Comment;
import me.inotsleep.utils.config.Path;

import java.io.File;

public class Config extends AbstractConfig {

    static {
        System.setProperty("file.encoding", "UTF-8");
    }

    @Comment("Turn off sound when the Minecraft window is not focused?")
    @Path("mute-on-alt-tab")
    public boolean muteOnAltTab = true;

    @Path("default-render-distance")
    public int maxDistance = 64;

    @Path("default-sync-display-volume")
    public double syncDisplayVolume = 0.25;

    @Path("default-default-display-volume")
    public double defaultDisplayVolume = 50;

    public Config(File baseDir) {
        super(baseDir, "config.yml");
    }
}