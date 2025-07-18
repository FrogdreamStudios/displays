package com.inotsleep.dreamdisplays;

import com.inotsleep.dreamdisplays.downloader.DependencyConfig;
import com.inotsleep.dreamdisplays.downloader.ytdlp.YtDlpDownloader;
import com.inotsleep.dreamdisplays.media.YtDlpExecutor;
import me.inotsleep.utils.logging.LoggingManager;

import java.nio.file.Path;
import java.util.logging.Logger;

public class TestRuntimeEntryPoint {
    public static void main(String[] args) {
        LoggingManager.setLogger(Logger.getLogger(TestRuntimeEntryPoint.class.getName()));

        DependencyConfig config = new DependencyConfig(TestRuntimeEntryPoint.class.getResourceAsStream("/dependencies.yml"));
        config.reload();
        Path ytDlpPath = YtDlpDownloader.download(config, Path.of("./target/yt-dlp"));

        System.out.println(ytDlpPath);

        if (ytDlpPath == null) return;

        YtDlpExecutor executor = new YtDlpExecutor(ytDlpPath);

        System.out.println(executor.getVideoLink("https://www.youtube.com/watch?v=HRfbQJ6FdF0", "480"));
        System.out.println(executor.getAudioLink("https://www.youtube.com/watch?v=HRfbQJ6FdF0"));
    }
}
