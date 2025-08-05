package com.inotsleep.dreamdisplays.client.downloader.ytdlp;

import com.inotsleep.dreamdisplays.client.downloader.DependencyConfig;
import com.inotsleep.dreamdisplays.client.downloader.Status;
import com.inotsleep.dreamdisplays.client.utils.Utils;
import me.inotsleep.utils.logging.LoggingManager;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

public class YtDlpDownloader {
    private static volatile Status status = Status.NOT_STARTED;
    private static volatile long downloadedBytes;
    private static volatile long totalBytes;
    private static volatile double percentage;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static Path download(DependencyConfig config, Path outputDir) {
        status = Status.STARTED;
        downloadedBytes = 0;
        totalBytes = -1;
        percentage = 0.0;

        String platform = Utils.getPlatform();
        String url      = config.ytdlp.get(platform);
        String sumUrl   = config.ytdlpChecksum;
        if (url == null || sumUrl == null) {
            status = Status.FAILED;
            LoggingManager.error("No yt-dlp URL or checksum for platform: " + platform);
            return null;
        }

        String fileName = Path.of(URI.create(url).getPath()).getFileName().toString();
        Path outputFile = outputDir.resolve(fileName);

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            status = Status.FAILED;
            LoggingManager.error("Cannot create output directory: " + outputDir, e);
            return null;
        }

        try {
            if (Files.exists(outputFile)) {
                String expected = fetchChecksum(sumUrl, fileName);
                if (expected != null) {
                    String actual = sha256(outputFile);
                    if (expected.equalsIgnoreCase(actual)) {
                        status = Status.COMPLETED;
                        totalBytes = Files.size(outputFile);
                        downloadedBytes = totalBytes;
                        percentage = 1.0;
                        LoggingManager.info("yt-dlp already present, checksum OK: " + outputFile);
                        return outputFile;
                    } else {
                        LoggingManager.warn("Checksum mismatch, re-downloading: "
                                + outputFile + " (got " + actual + ", expected " + expected + ")");
                        Files.delete(outputFile);
                    }
                }
            } else {
                LoggingManager.info("yt-dlp is not present. Downloading to: " + outputFile);
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            var resp = HTTP_CLIENT.send(req, BodyHandlers.ofInputStream());
            int code = resp.statusCode();
            if (code < 200 || code >= 300) {
                status = Status.FAILED;
                LoggingManager.error("Failed to download yt-dlp, HTTP status: " + code);
                return null;
            }

            Optional<String> len = resp.headers().firstValue("Content-Length");
            totalBytes = len.map(Long::parseLong).orElse(-1L);

            try (InputStream in = resp.body();
                 OutputStream out = Files.newOutputStream(outputFile)) {

                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                    downloadedBytes += read;
                    if (totalBytes > 0) {
                        percentage = (double) downloadedBytes / totalBytes;
                    }
                }
            }

            String expected = fetchChecksum(sumUrl, fileName);
            if (expected != null) {
                String actual = sha256(outputFile);
                if (!expected.equalsIgnoreCase(actual)) {
                    status = Status.FAILED;
                    LoggingManager.error("Checksum mismatch after download: got "
                            + actual + ", expected " + expected);
                    Files.delete(outputFile);
                    return null;
                }
            }

            status = Status.COMPLETED;
            percentage = 1.0;
            return outputFile;

        } catch (Exception e) {
            status = Status.FAILED;
            LoggingManager.error("Error downloading yt-dlp: " + e.getMessage(), e);
            return null;
        }
    }

    private static String fetchChecksum(String sumsUrl, String fileName) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(sumsUrl))
                    .GET()
                    .build();

            String body = HTTP_CLIENT.send(req, BodyHandlers.ofString()).body();
            List<String> lines = body.lines().toList();
            for (String line : lines) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2 && parts[1].equals(fileName)) {
                    return parts[0];
                }
            }
        } catch (Exception e) {
            LoggingManager.warn("Cannot fetch or parse checksum file: " + sumsUrl, e);
        }
        return null;
    }

    private static String sha256(Path path) {
        try (InputStream in = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance("SHA-256"))) {

            byte[] buf = new byte[8192];
            while (dis.read(buf) != -1) {
                // Nothing to do in this loop.
                // We are filling DigestInputStream
            }

            byte[] digest = dis.getMessageDigest().digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();

        } catch (Exception e) {
            LoggingManager.error("Error computing SHA-256 for " + path, e);
            return "";
        }
    }

    public static Status getStatus() {
        return status;
    }
    public static long getDownloadedBytes() {
        return downloadedBytes;
    }
    public static long getTotalBytes() {
        return totalBytes;
    }
    public static double getPercentage() {
        return percentage;
    }
}