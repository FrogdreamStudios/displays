package com.inotsleep.dreamdisplays;

import com.inotsleep.agent.JarLoader;
import com.inotsleep.dreamdisplays.downloader.DependencyConfig;
import com.inotsleep.dreamdisplays.downloader.ffmpeg.DependencyResolver;
import com.inotsleep.dreamdisplays.downloader.maven.MavenResolver;
import com.inotsleep.dreamdisplays.downloader.ytdlp.YtDlpDownloader;
import com.inotsleep.dreamdisplays.media.ytdlp.YtDlpExecutor;
import me.inotsleep.utils.logging.LoggingManager;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class TestRuntimeEntryPoint {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TestRuntimeEntryPoint.class);

    public static void main(String[] args) throws Exception {
        LoggingManager.setLogger(Logger.getLogger(TestRuntimeEntryPoint.class.getName()));
        DependencyConfig config = new DependencyConfig(TestRuntimeEntryPoint.class.getResourceAsStream("/dependencies.yml"));
        config.reload();
        Path ytDlpPath = YtDlpDownloader.download(config, Path.of("./target/yt-dlp"));

        List<Path> libs = DependencyResolver.resolve(new MavenResolver(Path.of("./target/libs"), config.repositories), config);

        System.out.println(ytDlpPath);

        if (ytDlpPath == null) return;

        new YtDlpExecutor(ytDlpPath);
//        AudioVideoPlayerWrapper player = new AudioVideoPlayerWrapper(loader, "3UCI4cUFlVs");
//        player.initialize();

        JarLoader.loadLibrariesAtRuntime(libs.stream().map(Path::toString).toArray(String[]::new));

        MediaRunner.run();
    }
}
