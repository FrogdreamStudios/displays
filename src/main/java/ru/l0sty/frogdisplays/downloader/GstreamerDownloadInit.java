package ru.l0sty.frogdisplays.downloader;

import me.inotsleep.utils.LoggerFactory;
import org.freedesktop.gstreamer.Gst;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;


public class GstreamerDownloadInit {
    private static void setupLibraryPath() throws IOException {
        final File gStreamerLibrariesDir = new File("./libs/gstreamer");

        List<File> files = List.of(Objects.requireNonNull(new File(gStreamerLibrariesDir, "bin").listFiles()));

        GStreamerDownloadListener.INSTANCE.setProgress(0f);
        GStreamerDownloadListener.INSTANCE.setTask("Загрузка библиотек 0/0");
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

    public static void loadLibraries(Collection<String> libraries) {
        Deque<String> toLoad = new ArrayDeque<>(libraries);
        int total = libraries.size();
        int loadedCount = 0;

        GStreamerDownloadListener.INSTANCE.setTask(String.format("Загрузка библиотек %d/%d", loadedCount, total));
        while (!toLoad.isEmpty()) {
            int passSize = toLoad.size();
            int loadedThisPass = 0;

            // Пробуем загрузить каждый из оставшихся
            for (int i = 0; i < passSize; i++) {
                String path = toLoad.removeFirst();
                try {
                    System.load(path);
                    loadedCount++;
                    loadedThisPass++;
                    // обновляем прогресс: сколько из общего уже загружено
                    GStreamerDownloadListener.INSTANCE
                            .setProgress(((float) loadedCount) / total);

                    GStreamerDownloadListener.INSTANCE.setTask(String.format("Загрузка библиотек %d/%d (%d/%d)", loadedCount, total, loadedThisPass, passSize));
                } catch (LinkageError e) {
                    toLoad.addLast(path);
                }
            }

            // Если за весь проход ни одной не загрузилось — выходим, чтобы не зациклиться
            if (loadedThisPass == 0) {
                LoggerFactory.getLogger().severe("Не удалось загрузить следующие библиотеки:");
                toLoad.forEach(p -> LoggerFactory.getLogger().severe("  " + p));

                GStreamerDownloadListener.INSTANCE.setFailed(true);
                return;
            }
        }

        GStreamerDownloadListener.INSTANCE.setDone(true);
    }

    private static final Pattern SO_PATTERN =
            Pattern.compile(".*\\.so(\\.\\d+)*$", Pattern.CASE_INSENSITIVE);
    // Паттерн для .dylib и старых .jnilib на macOS
    private static final Pattern DYLIB_PATTERN =
            Pattern.compile(".*\\.(dylib|jnilib)$", Pattern.CASE_INSENSITIVE);

    /**
     * Проверяет, выглядит ли имя файла как нативная библиотека
     * для текущей ОС.
     * Поддерживается:
     *   Windows — .dll
     *   Linux/Unix — .so и .so.<version>
     *   macOS — .dylib, .jnilib
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
        // Linux/Unix (включая версии .so.1, .so.1.2 и т.п.)
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

    public static void sinit() {
        final File gStreamerLibrariesDir = new File("./libs/gstreamer");
        if (!gStreamerLibrariesDir.exists() && gStreamerLibrariesDir.mkdirs()) LoggerFactory.getLogger().severe("Unable to mk directory");

        Thread downloadThread = new Thread(() -> {
            GStreamerDownloader downloader = new GStreamerDownloader();
            boolean downloadGStreamer;

            try {
                downloadGStreamer = !downloader.downloadGstreamerChecksum();
            } catch (IOException e) {
                LoggerFactory.getLogger().log(Level.SEVERE, "Failed to download GStreamer checksum.", e);
                GStreamerDownloadListener.INSTANCE.setFailed(true);
                return;
            }

            File gStreamerBinLibrariesDir = new File("./libs/gstreamer/bin");
            downloadGStreamer |= !gStreamerBinLibrariesDir.exists();

            if (downloadGStreamer) {
                try {
                    downloader.downloadGstreamerBuild();
                } catch (IOException e) {
                    LoggerFactory.getLogger().log(Level.SEVERE,"Failed to download GStreamer.", e);
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