package ru.l0sty.frogdisplays;

import me.inotsleep.utils.config.AbstractConfig;
import me.inotsleep.utils.config.Comment;
import me.inotsleep.utils.config.Path;

import java.io.File;

public class Config extends AbstractConfig {

    static {
        System.setProperty("file.encoding", "UTF-8");
    }

    @Comment("Turn off the displays when the Minecraft window is not focused?")

    @Path("mute-on-alt-tab")
    public boolean muteOnAltTab = true;

    public Config(File baseDir) {
        super(baseDir, "config.yml");
    }
}