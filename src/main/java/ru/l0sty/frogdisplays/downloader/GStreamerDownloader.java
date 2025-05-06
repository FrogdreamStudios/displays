package ru.l0sty.frogdisplays.downloader;

import me.inotsleep.utils.LoggerFactory;
import org.apache.commons.io.FileUtils;
import ru.l0sty.frogdisplays.util.Utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GStreamerDownloader {
    private static final String JAVA_CEF_DOWNLOAD_URL = "https://dl.frogdream.xyz/gstreamer-${platform}.zip";
    private static final String JAVA_CEF_CHECKSUM_DOWNLOAD_URL = JAVA_CEF_DOWNLOAD_URL + ".sha256";

    public GStreamerDownloader() {
    }

    public String getGStreamerDownloadUrl() {
        return formatURL(JAVA_CEF_DOWNLOAD_URL);
    }

    public String getGStreamerChecksumDownloadUrl() {
        return formatURL(JAVA_CEF_CHECKSUM_DOWNLOAD_URL);
    }

    private String formatURL(String url) {
        // Get system platform


        return url
                .replace("${platform}", Utils.detectPlatform());
    }

    public void downloadGstreamerBuild() throws IOException {
        File gStreamerLibrariesPath = new File("./libs/gstreamer");
        GStreamerDownloadListener.INSTANCE.setTask("Downloading GStreamer");
        downloadFile(getGStreamerDownloadUrl(), new File(gStreamerLibrariesPath,  "gstreamer.zip"));
    }

    public boolean downloadGstreamerChecksum() throws IOException {
        File gStreamerLibrariesPath = new File("./libs/gstreamer");
        File gStreamerHashFileTemp = new File(gStreamerLibrariesPath, "gstreamer.zip.sha256.temp");
        File gStreamerHashFile = new File(gStreamerLibrariesPath, "gstreamer.zip.sha256");

        GStreamerDownloadListener.INSTANCE.setTask("Downloading Checksum");
        downloadFile(getGStreamerChecksumDownloadUrl(), gStreamerHashFileTemp);

        if (gStreamerHashFile.exists()) {
            boolean sameContent = FileUtils.contentEquals(gStreamerHashFile, gStreamerHashFileTemp);
            if (sameContent) {
                if (!gStreamerHashFileTemp.delete()) LoggerFactory.getLogger().warning("Unable to delete directory");
                return true;
            } else {
                LoggerFactory.getLogger().warning("GStreamer Hash does not match.");
            }
        } else {
            LoggerFactory.getLogger().warning("Failed to download GStreamer hash.");
        }

        if (!gStreamerHashFileTemp.renameTo(gStreamerHashFile)) LoggerFactory.getLogger().warning("Unable to rename directory");

        return false;
    }

    public void extractGstreamer(boolean delete) {
        File gStreamerLibrariesPath = new File("./libs/gstreamer");
        File tarGzArchive = new File(gStreamerLibrariesPath, "gstreamer.zip");
        extractZip(tarGzArchive, gStreamerLibrariesPath);
        if (delete) {
            if (tarGzArchive.exists()) {
                if (!tarGzArchive.delete()) LoggerFactory.getLogger().warning("Unable to delete file");
            }
        }
    }

    private static void downloadFile(String urlString, File outputFile) throws IOException {
        try {
            LoggerFactory.getLogger().info(urlString + " -> " + outputFile.getCanonicalPath());

            URL url = URL.of(new URI(urlString), null);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            if (urlConnection.getResponseCode() != 200) {
                throw new IOException();
            }

            int fileSize = urlConnection.getContentLength();

            BufferedInputStream inputStream = new BufferedInputStream(url.openStream());
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[2048];
            int count;
            int readBytes = 0;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
                readBytes += count;
                float percentComplete = (float) readBytes / fileSize;
                GStreamerDownloadListener.INSTANCE.setProgress(percentComplete);
                buffer = new byte[Math.max(2048, inputStream.available())];
            }

            inputStream.close();
            outputStream.close();
        } catch (URISyntaxException | IOException e) {
            throw new IOException("Failed to download " + urlString);
        }
    }

    private static void extractZip(File zipFile, File outputDirectory) {
        GStreamerDownloadListener.INSTANCE.setTask("Extracting");
        if (!outputDirectory.getParentFile().mkdirs()) LoggerFactory.getLogger().warning("Unable to mk directory");

        long fileSize = zipFile.length();
        long totalBytesRead = 0;

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File outputFile = new File(outputDirectory, entry.getName());

                if (entry.isDirectory()) {
                    if (!outputFile.getParentFile().mkdirs()) LoggerFactory.getLogger().warning("Unable to mk directory");
                    continue;
                }

                // Создаём папки, если нужно
                if (!outputFile.getParentFile().mkdirs()) LoggerFactory.getLogger().warning("Unable to mk directory");

                try (InputStream inputStream = zip.getInputStream(entry);
                     OutputStream outputStream = new FileOutputStream(outputFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        // Обновляем прогресс
                        float percentComplete = (float) totalBytesRead / fileSize;
                        GStreamerDownloadListener.INSTANCE.setProgress(percentComplete);
                    }
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger().log(Level.SEVERE,
                    "Failed to extract zip file to " + outputDirectory, e);
        }

        // Обязательно в конце выставляем 100%
        GStreamerDownloadListener.INSTANCE.setProgress(1.0f);
    }
}