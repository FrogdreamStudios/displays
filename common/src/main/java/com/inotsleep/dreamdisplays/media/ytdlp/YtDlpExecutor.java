package com.inotsleep.dreamdisplays.media.ytdlp;

import com.google.gson.*;
import me.inotsleep.utils.Pair;
import me.inotsleep.utils.logging.LoggingManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YtDlpExecutor {
    Path ytDlpPath;
    private static YtDlpExecutor instance;

    private final YouTubeQueryCache videoCache = new YouTubeQueryCache();

    public static YtDlpExecutor getInstance() {
        return instance;
    }

    public YtDlpExecutor(Path ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
        File ytDlpFile = new File(ytDlpPath.toFile().getAbsolutePath());
        if (ytDlpFile.exists()) {
            ytDlpFile.setExecutable(true, true);
        }
        instance = this;
    }

    public YouTubeCacheEntry getFormats(String videoCode) {
        YouTubeCacheEntry cachedVideo = videoCache.get(videoCode);
        if (cachedVideo != null) {
            return cachedVideo;
        }

        Pair<List<String>, List<String>> result = null;
        try {
            result = execute(
                    ytDlpPath.toString(),
                    "-j",
                    "--skip-download",
                    "https://www.youtube.com/watch?v=" + videoCode
            );
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

        String json = stdout.getFirst();

        JsonElement videoInfo = JsonParser.parseString(json);
        JsonArray formatsArray = videoInfo.getAsJsonObject().get("formats").getAsJsonArray();

        Gson gson = new Gson();
        List<Format> formats = new ArrayList<>(formatsArray.size());

        for (JsonElement elem : formatsArray) {
            Format f = gson.fromJson(elem, Format.class);
            formats.add(f);
        }

        formats = formats
                .stream()
                .filter(
                    format -> {
                        String aCodec = format.getAcodec();
                        String vCodec = format.getVcodec();
                        String container = format.getContainer();
                        // Remove formats without video and audio. That also removes hls audio
                        if ((aCodec == null || aCodec.equals("none")) && (vCodec == null || vCodec.equals("none"))) return false;

                        // Remove formats that have both video and audio. This also excludes progressive http-mp4
                        if ((aCodec != null && !aCodec.equals("none")) && (vCodec != null && !vCodec.equals("none"))) return false;

                        // Remove non-dash formats
                        if (container == null || !container.contains("dash")) return false;
                        return true;
                    }
                )
                .toList();

        YouTubeCacheEntry video = new YouTubeCacheEntry(formats);
        videoCache.put(videoCode, video);
        return video;
    }

    @Deprecated
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

    @Deprecated
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
