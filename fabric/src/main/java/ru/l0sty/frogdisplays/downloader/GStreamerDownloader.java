package ru.l0sty.frogdisplays.downloader;

import me.inotsleep.utils.logging.LoggingManager;
import org.apache.commons.io.FileUtils;
import ru.l0sty.frogdisplays.util.Utils;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GStreamerDownloader {

    /**
     * Download URL for GStreamer.
     * In future versions we'll delete this and use the official GStreamer repository to avoid some issues.
     */
    private static final String GSTREAMER_DOWNLOAD_URL = "https://dl.frogdream.xyz/gstreamer-${platform}.zip";
    private static final String GSTREAMER_CHECKSUM_DOWNLOAD_URL = GSTREAMER_DOWNLOAD_URL + ".sha256";

    public GStreamerDownloader() {}

    public String getGStreamerDownloadUrl() {
        return formatURL(GSTREAMER_DOWNLOAD_URL);
    }

    public String getGStreamerChecksumDownloadUrl() {
        return formatURL(GSTREAMER_CHECKSUM_DOWNLOAD_URL);
    }

    private String formatURL(String url) {
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
                if (!gStreamerHashFileTemp.delete()) LoggingManager.warn("Unable to delete directory");
                return true;
            } else {
                LoggingManager.warn("GStreamer Hash does not match.");
            }
        } else {
            LoggingManager.warn("Failed to download GStreamer hash.");
        }

        if (!gStreamerHashFileTemp.renameTo(gStreamerHashFile)) LoggingManager.warn("Unable to rename directory");

        return false;
    }

    public void extractGstreamer(boolean delete) {
        File gStreamerLibrariesPath = new File("./libs/gstreamer");
        File tarGzArchive = new File(gStreamerLibrariesPath, "gstreamer.zip");
        extractZip(tarGzArchive, gStreamerLibrariesPath);
        if (delete) {
            if (tarGzArchive.exists()) {
                if (!tarGzArchive.delete()) LoggingManager.warn("Unable to delete file");
            }
        }
    }

    /**
     * Downloads a file from the specified URL and saves it to the given output file.
     * @param urlString the URL of the file to download.
     */
    private static void downloadFile(String urlString, File outputFile) throws IOException {
        LoggingManager.info(urlString + " -> " + outputFile.getCanonicalPath());

        HttpURLConnection conn = null;
        InputStream  in  = null;
        FileOutputStream out = null;

        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Java/" + System.getProperty("java.version"));
            conn.setRequestProperty("Accept", "application/octet-stream");

            int status = conn.getResponseCode();
            if (status / 100 == 3) {
                String redirectUrl = conn.getHeaderField("Location");
                conn.disconnect();
                conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Java/" + System.getProperty("java.version"));
                status = conn.getResponseCode();
            }

            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + status + " for " + urlString);
            }

            int fileSize = conn.getContentLength(); // May be -1
            in = new BufferedInputStream(conn.getInputStream());
            out = new FileOutputStream(outputFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (fileSize > 0) {
                    float percent = (float) totalRead / fileSize;
                    GStreamerDownloadListener.INSTANCE.setProgress(percent);
                }
            }
        } catch (MalformedURLException e) {
            throw new IOException("Bad URL: " + urlString, e);
        } finally {
            if (in  != null) try { in.close();  } catch (IOException ignored) {}
            if (out != null) try { out.close(); } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Extracts a ZIP file to the specified output directory.
     */
    private static void extractZip(File zipFile, File outputDirectory) {
        GStreamerDownloadListener.INSTANCE.setTask("Extracting");
        if (!outputDirectory.getParentFile().exists() && !outputDirectory.getParentFile().mkdirs()) LoggingManager.warn("Unable to mk directory");

        long fileSize = zipFile.length();
        long totalBytesRead = 0;

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File outputFile = new File(outputDirectory, entry.getName());


                if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) LoggingManager.warn("Unable to mk directory");

                if (entry.isDirectory()) {
                    continue;
                }

                try (InputStream inputStream = zip.getInputStream(entry);
                     OutputStream outputStream = new FileOutputStream(outputFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        float percentComplete = (float) totalBytesRead / fileSize;
                        GStreamerDownloadListener.INSTANCE.setProgress(percentComplete);
                    }
                }
            }
        } catch (IOException e) {
            LoggingManager.error("Failed to extract zip file to " + outputDirectory, e);
        }

        GStreamerDownloadListener.INSTANCE.setProgress(1.0f);
    }
}