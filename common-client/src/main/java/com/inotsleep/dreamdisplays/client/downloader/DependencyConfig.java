package com.inotsleep.dreamdisplays.client.downloader;

import me.inotsleep.utils.config.AbstractConfig;
import me.inotsleep.utils.config.Path;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class DependencyConfig extends AbstractConfig {
        public DependencyConfig(InputStream stream) { super(stream); }

        @Path("repositories")
        public Map<String, String> repositories;

        @Path("dependencies")
        public List<String> dependencies;

        @Path("yt-dlp")
        public Map<String, String> ytdlp;

        @Path("yt-dlp-checksum")
        public String ytdlpChecksum;
}