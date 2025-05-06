package ru.l0sty.frogdisplays.downloader;

import me.inotsleep.utils.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;


public class CefDownloadMixin {
    private static void setupLibraryPath() throws IOException {
// Disabled as not working :( TODO: FIX
//        final File gStreamerLibrariesDir = new File("./libs/gstreamer");
//        if (!gStreamerLibrariesDir.exists()) gStreamerLibrariesDir.mkdirs();
//        System.setProperty("jna.library.path", String.join(File.pathSeparator, new File(gStreamerLibrariesDir, "bin").getCanonicalPath()/*, new File(gStreamerLibrariesDir, "lib")
//                new File("C:/Program Files/gstreamer/1.0/mingw_x86_64/bin").getCanonicalPath()*/));
//        System.setProperty("GST_PLUGIN_SYSTEM_PATH",
//                new File(gStreamerLibrariesDir, "lib/gstreamer-1.0").getCanonicalPath());
//        System.setProperty("GST_PLUGIN_PATH",
//                new File(gStreamerLibrariesDir, "lib/gstreamer-1.0").getCanonicalPath());

    }

    public static void sinit() {
        try {
            setupLibraryPath();
        } catch (IOException e) {
            LoggerFactory.getLogger().log(Level.SEVERE, "Could not setup library path", e);
        }

        Thread downloadThread = new Thread(() -> {
            GStreamerDownloader downloader = new GStreamerDownloader();
            boolean downloadGStreamer;

            try {
                downloadGStreamer = !downloader.downloadGstreamerChecksum();
            } catch (IOException e) {
                LoggerFactory.getLogger().log(Level.SEVERE, "Failed to download JCEF checksum.", e);
                GStreamerDownloadListener.INSTANCE.setFailed(true);
                return;
            }

            // Ensure the mcef-libraries directory exists
            // If not, we want to try redownloading
            File mcefLibrariesDir = new File("./libs/gstreamer");
            downloadGStreamer |= !mcefLibrariesDir.exists();

            if (downloadGStreamer) {
                try {
                    downloader.downloadGstreamerBuild();
                } catch (IOException e) {
                    LoggerFactory.getLogger().log(Level.SEVERE,"Failed to download JCEF.", e);
                    GStreamerDownloadListener.INSTANCE.setFailed(true);
                    return;
                }

                downloader.extractGstreamer(true);
            }

            GStreamerDownloadListener.INSTANCE.setDone(true);
        });
        downloadThread.start();
    }
}