package ru.l0sty.frogdisplays.downloader;

import me.inotsleep.utils.logging.LoggingManager;
import org.freedesktop.gstreamer.Gst;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static ru.l0sty.frogdisplays.util.Utils.detectPlatform;

public class GstreamerDownloadInit {

    /**
     * Initializes GStreamer libraries for the application.
     */
    private static void setupLibraryPath() throws IOException {
        final File gStreamerLibrariesDir = new File("./libs/gstreamer");

        List<File> files = List.of(Objects.requireNonNull(new File(gStreamerLibrariesDir, "bin").listFiles()));

        GStreamerDownloadListener.INSTANCE.setProgress(0f);
        GStreamerDownloadListener.INSTANCE.setTask("Loading libraries for Dream Launcher 0/0");
        loadLibraries(recursiveLoadLibs(files));

        System.setProperty(
                "jna.library.path",
                String.join(
                        File.pathSeparator,
                        new File(gStreamerLibrariesDir, "bin")
                                .getCanonicalPath(),
                        new File(gStreamerLibrariesDir, "lib")
                                .getCanonicalPath()
                )
        );
        Gst.init("MediaPlayer");
    }

    /**
     * Loads the specified libraries into the JVM.
     */
    public static void loadLibraries(Collection<String> libraries) {
        Deque<String> toLoad = new ArrayDeque<>(libraries);
        int total = libraries.size();
        int loadedCount = 0;

        GStreamerDownloadListener.INSTANCE.setTask(String.format("Loading libraries for Dream Displays %d/%d", loadedCount, total));
        while (!toLoad.isEmpty()) {
            int passSize = toLoad.size();
            int loadedThisPass = 0;

            // Try to load other libraries.
            for (int i = 0; i < passSize; i++) {
                String path = toLoad.removeFirst();
                try {
                    System.load(path);
                    loadedCount++;
                    loadedThisPass++;

                    // Update progress and task message
                    GStreamerDownloadListener.INSTANCE
                            .setProgress(((float) loadedCount) / total);

                    GStreamerDownloadListener.INSTANCE.setTask(String.format("Loading libraries for Dream Displays %d/%d (%d/%d)", loadedCount, total, loadedThisPass, passSize));
                } catch (LinkageError e) {
                    toLoad.addLast(path);
                }
            }

            if (loadedThisPass == 0) {
                LoggingManager.error("Dream Displays can't load some libraries:");
                toLoad.forEach(p -> LoggingManager.error("  " + p));

                GStreamerDownloadListener.INSTANCE.setFailed(true);
                return;
            }
        }

        GStreamerDownloadListener.INSTANCE.setDone(true);
    }

    private static final Pattern SO_PATTERN =
            Pattern.compile(".*\\.so(\\.\\d+)*$", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for macOS dynamic libraries and old libraries.
     */
    private static final Pattern DYLIB_PATTERN =
            Pattern.compile(".*\\.(dylib|jnilib)$", Pattern.CASE_INSENSITIVE);

    /**
     * Check if the file name is a library.
     * @param name the file name to check
     * @return true if the file name is a library, false otherwise
     */
    private static boolean isLib(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        // Windows
        if (lower.endsWith(".dll")) {
            return true;
        }
        // macOS
        if (DYLIB_PATTERN.matcher(name).matches()) {
            return true;
        }
        // Linux/Unix (including .so.1, .so.1.2, etc.)
        return SO_PATTERN.matcher(name).matches();
    }

    private static List<String> recursiveLoadLibs(List<File> files) {
        List<String> libs = new ArrayList<>();

        for (File file : files) {
            if (isLib(file.getName())) libs.add(file.getAbsolutePath());
            else if (file.isDirectory()) libs.addAll(recursiveLoadLibs(List.of(Objects.requireNonNull(file.listFiles()))));
        }

        return libs;
    }

    public static void init() {
        String platform = detectPlatform();
        if (!platform.equals("windows")) {
            GStreamerDownloadListener.INSTANCE.setFailed(true);
            return;
        }


        final File gStreamerLibrariesDir = new File("./libs/gstreamer");
        if (!gStreamerLibrariesDir.exists() && gStreamerLibrariesDir.mkdirs()) LoggingManager.error("Unable to mk directory");

        Thread downloadThread = new Thread(() -> {
            GStreamerDownloader downloader = new GStreamerDownloader();
            boolean downloadGStreamer;

            try {
                downloadGStreamer = !downloader.downloadGstreamerChecksum();
            } catch (IOException e) {
                LoggingManager.error("Failed to download GStreamer checksum.", e);
                GStreamerDownloadListener.INSTANCE.setFailed(true);
                return;
            }

            File gStreamerBinLibrariesDir = new File("./libs/gstreamer/bin");
            downloadGStreamer |= !gStreamerBinLibrariesDir.exists();

            if (downloadGStreamer) {
                try {
                    downloader.downloadGstreamerBuild();
                } catch (IOException e) {
                    LoggingManager.error("Failed to download GStreamer.", e);
                    GStreamerDownloadListener.INSTANCE.setFailed(true);
                    return;
                }

                downloader.extractGstreamer(true);
            }

            try {
                setupLibraryPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
        downloadThread.start();
    }
}