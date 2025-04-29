package ru.l0sty.frogdisplays;

import me.inotsleep.utils.config.AbstractConfig;
import me.inotsleep.utils.config.Comment;
import me.inotsleep.utils.config.Path;

import java.io.File;

public class Config extends AbstractConfig {

    static {
        System.setProperty("file.encoding", "UTF-8");
    }

    @Comment("Отключать звук видео если игра свёрнутая?")
    @Path("mute-on-alt-tab")
    public boolean muteOnAltTab = true;

    public Config(File baseDir) {
        super(baseDir, "config.yml");
    }
}
