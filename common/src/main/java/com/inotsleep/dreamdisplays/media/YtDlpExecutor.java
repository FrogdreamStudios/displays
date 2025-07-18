package com.inotsleep.dreamdisplays.media;

import me.inotsleep.utils.Pair;
import me.inotsleep.utils.logging.LoggingManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YtDlpExecutor {
    Path ytDlpPath;

    public YtDlpExecutor(Path ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }

    public String getVideoLink(String url, String quality) {
        return getYtDlpLink(
                ytDlpPath.toString(),
                "-f",
                "bestvideo[protocol^=m3u8_native][height<=" + quality + "]",
                "--get-url",
                "--skip-download",
                url
        );
    }

    public String getAudioLink(String url) {
        return getYtDlpLink(
                ytDlpPath.toString(),
                "-f",
                "bestaudio[protocol^=m3u8_native]",
                "--get-url",
                "--skip-download",
                url
        );
    }

    private String getYtDlpLink(String ...command) {
        Pair<List<String>, List<String>> result = null;
        try {
            result = execute(command);
        } catch (IOException | InterruptedException e) {
            LoggingManager.error("Unable to get video link", e);
            return null;
        }

        List<String> stdout = result.getK();
        List<String> stderr = result.getV();

        if (stdout.isEmpty()) {
            LoggingManager.error("Unable to get video link");
            stderr.forEach(LoggingManager::error);

            return null;
        }

        if (!stderr.isEmpty()) {
            stderr.forEach(LoggingManager::error);
        }

        String videoLink = stdout.getFirst();
        if (videoLink.isEmpty()) {
            LoggingManager.error("Unable to get video link");
        }

        try {
            URI uri = URI.create(videoLink);
        } catch (IllegalArgumentException e) {
            LoggingManager.error("Unable to get video link", e);
            LoggingManager.error("Video link: " + videoLink);

            return null;
        }

        return videoLink;
    }

    private Pair<List<String>, List<String>> execute(String ...command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        Process process = processBuilder.start();

        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();

        Runnable outReader = () -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stdout.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        Runnable errReader = () -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderr.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(outReader);
        pool.submit(errReader);

        process.waitFor();
        pool.close();

        return new Pair<>(stdout, stderr);
    }
}
