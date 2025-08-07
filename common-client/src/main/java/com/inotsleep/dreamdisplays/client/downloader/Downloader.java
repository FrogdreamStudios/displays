package com.inotsleep.dreamdisplays.client.downloader;

import com.inotsleep.dreamdisplays.client.agent.JarLoader;
import com.inotsleep.dreamdisplays.client.downloader.ffmpeg.DependencyResolver;
import com.inotsleep.dreamdisplays.client.downloader.maven.MavenResolver;
import com.inotsleep.dreamdisplays.client.downloader.ytdlp.YtDlpDownloader;
import com.inotsleep.dreamdisplays.client.media.ytdlp.YtDlpExecutor;
import me.inotsleep.utils.logging.LoggingManager;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.DependencyResolutionException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Downloader {
    private final Path libratyPath;
    private final List<Task> tasks;
    private int currentTaskIndex = 0;

    private boolean done = false;
    private boolean isFailed = false;

    private static Downloader instance;

    DependencyConfig config;

    public Downloader(Path libraryPath) {
        instance = this;

        this.libratyPath = libraryPath;
        config = new DependencyConfig(Downloader.class.getResourceAsStream("/dependencies.yml"));
        config.reload();
        tasks = new ArrayList<>();

        tasks.add(new Task("Downloading ffmpeg libraries") {
            MavenResolver resolver;

            Map<String, Long> estimatedSizes;
            long totalSize;

            @Override
            public List<ArtifactDownloadingProgress> getProgress() {
                if (resolver == null) return List.of();

                return resolver
                        .getArtifactProgressMap()
                        .entrySet()
                        .stream()
                        .map(
                                (entry) ->
                                        new ArtifactDownloadingProgress(
                                            entry.getKey(),
                                            entry.getValue(),
                                            (long) (estimatedSizes.getOrDefault(entry.getKey(), 0L) * entry.getValue()),
                                            estimatedSizes.getOrDefault(entry.getKey(), 0L)
                                        )
                                )
                        .toList();
            }

            @Override
            public Status getStatus() {
                return resolver.getDownloadStatus();
            }

            @Override
            public ArtifactDownloadingProgress getTotalProgress() {
                return new ArtifactDownloadingProgress(
                        getName(),
                        (double) resolver.getDownloadedBytes() / totalSize,
                        resolver.getDownloadedBytes(),
                        totalSize
                );
            }

            @Override
            public void execute() {
                try {
                    JarLoader.loadLibrariesAtRuntime(DependencyResolver.resolve(resolver, config));
                } catch (DependencyResolutionException | IOException e) {
                    LoggingManager.error("Failed to download libraries", e);
                    fail();
                }
            }

            @Override
            public void init() {
                resolver = new MavenResolver(libratyPath.resolve("maven"));
                resolver.addRepositories(config.repositories);

                try {
                    estimatedSizes = DependencyResolver.getEstimatedSize(resolver, config);
                    totalSize = estimatedSizes.values().stream().reduce(0L, Long::sum);
                } catch (DependencyCollectionException e) {
                    LoggingManager.error("Failed to get estimated size", e);
                    fail();
                }
            }
        });

        tasks.add(new Task("Downloading yt-dlp") {
            @Override
            public List<ArtifactDownloadingProgress> getProgress() {
                return List.of(
                        new ArtifactDownloadingProgress(
                        "yt-dlp executable",
                        YtDlpDownloader.getPercentage(),
                        YtDlpDownloader.getDownloadedBytes(),
                        YtDlpDownloader.getTotalBytes()
                ));
            }

            @Override
            public Status getStatus() {
                return YtDlpDownloader.getStatus();
            }

            @Override
            public ArtifactDownloadingProgress getTotalProgress() {
                return new ArtifactDownloadingProgress(
                        getName(),
                        YtDlpDownloader.getPercentage(),
                        YtDlpDownloader.getDownloadedBytes(),
                        YtDlpDownloader.getTotalBytes()
                );
            }

            @Override
            public void execute() {
                Path ytDlpPath = YtDlpDownloader.download(config, libratyPath.resolve("yt-dlp"));

                if (ytDlpPath == null) {
                    LoggingManager.error("Failed to download yt-dlp");
                    fail();
                    return;
                }

                new YtDlpExecutor(ytDlpPath);
            }

            @Override
            public void init() {}
        });
    }

    public static Downloader getInstance() {
        return instance;
    }

    public static void fail() {
        instance.isFailed = true;
    }

    public void startDownload() {
        for (; currentTaskIndex < tasks.size(); currentTaskIndex++) {
            Task task = tasks.get(currentTaskIndex);
            LoggingManager.info("Downloading task " + task.getName());
            task.execute();
        }

        done = !isFailed;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public Task getTask() {
        if (currentTaskIndex >= tasks.size()) {
            return new Task("Loading download list...") {
                @Override
                public List<ArtifactDownloadingProgress> getProgress() {
                    return List.of();
                }

                @Override
                public Status getStatus() {
                    return Status.NOT_STARTED;
                }

                @Override
                public ArtifactDownloadingProgress getTotalProgress() {
                    return new ArtifactDownloadingProgress("", 0, 0, 0);
                }

                @Override
                public void execute() {

                }

                @Override
                public void init() {

                }
            };
        }
        return tasks.get(currentTaskIndex);
    }

    public double getProgress() {
        double result = getTask().getTotalProgress().progress() / tasks.size()
                + (double) currentTaskIndex / Math.max(tasks.size(), 1);
        return Double.isNaN(result) ? 0.0 : result;
    }

    public static abstract class Task {
        private final String name;

        public Task(String name) {
            this.name = name;
            init();
        }

        public String getName() {
            return name;
        }

        public abstract List<ArtifactDownloadingProgress> getProgress();
        public abstract Status getStatus();
        public abstract ArtifactDownloadingProgress getTotalProgress();
        public abstract void execute();
        public abstract void init();
    }

    public record ArtifactDownloadingProgress(String name, double progress, long downloadedBytes, long totalBytes) {}
}
