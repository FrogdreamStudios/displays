package com.inotsleep.dreamdisplays.downloader.maven;

import com.inotsleep.dreamdisplays.downloader.Status;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class MavenResolver {
    private final RepositorySystem system;
    private final DefaultRepositorySystemSession session;
    private final Map<String, RemoteRepository> reposById;

    private volatile Status downloadStatus = Status.NOT_STARTED;

    private final ConcurrentMap<String, Double> artifactProgressMap = new ConcurrentHashMap<>();
    private final AtomicLong downloadedBytes = new AtomicLong(0);

    public MavenResolver(Path baseDir, Map<String, String> repositories) {
        this(baseDir);
        addRepositories(repositories);
    }

    public MavenResolver(Path baseDir) {
        system = new RepositorySystemSupplier().get();
        SessionBuilderSupplier supplier = new SessionBuilderSupplier(system);
        RepositorySystemSession baseSession = MavenRepositorySystemUtils.newSession();
        session = new DefaultRepositorySystemSession(supplier.get()
                .setLocalRepositoryManager(
                        system.newLocalRepositoryManager(
                                baseSession,
                                new LocalRepository(baseDir)
                        )
                )
                .build());

        session.setTransferListener(new TransferListener() {
            @Override
            public void transferInitiated(TransferEvent event) {
                String res = event.getResource().getPath().getFileName().toString();
                downloadStatus = Status.STARTED;
                artifactProgressMap.put(res, 0.0);
            }

            @Override
            public void transferStarted(TransferEvent event) {
                downloadStatus = Status.STARTED;
            }

            @Override
            public void transferProgressed(TransferEvent event) {
                TransferResource res = event.getResource();
                String name = res.getPath().getFileName().toString();
                long transferred = event.getTransferredBytes();
                long total = res.getContentLength();

                downloadedBytes.addAndGet((long) (transferred - total * artifactProgressMap.get(name)));

                if (total > 0) {
                    artifactProgressMap.put(name, (double) transferred / total);
                }
            }

            @Override
            public void transferSucceeded(TransferEvent event) {

            }

            @Override
            public void transferFailed(TransferEvent event) {
                downloadStatus = Status.FAILED;
                String res = event.getResource().getPath().getFileName().toString();
                artifactProgressMap.put(res, -1.0);
            }

            @Override
            public void transferCorrupted(TransferEvent event) {
                downloadStatus = Status.FAILED;
                String res = event.getResource().getPath().getFileName().toString();
                artifactProgressMap.put(res, -1.0);
            }
        });

        this.reposById = new java.util.LinkedHashMap<>();
    }

    public void addRepository(RemoteRepository repository) {
        reposById.putIfAbsent(repository.getId(), repository);
    }

    public void addRepository(String id, String url) {
        reposById.put(id,
                new RemoteRepository.Builder(id, "default", url).build()
        );
    }

    public void addRepositories(Map<String, String> repositories) {
        repositories.forEach(this::addRepository);
    }

    public List<RemoteRepository> getRemoteRepositories() {
        return List.copyOf(reposById.values());
    }

    public boolean removeRepository(String id) {
        return reposById.remove(id) != null;
    }

    public void removeRepository(RemoteRepository repository) {
        reposById.remove(repository.getId());
    }

    public void clearRepositories() {
        reposById.clear();
    }

    public RepositorySystem getSystem() {
        return system;
    }

    public RepositorySystemSession getSession() {
        return session;
    }

    public Status getDownloadStatus() {
        return downloadStatus;
    }

    public double getArtifactProgress(String resourceName) {
        return artifactProgressMap.getOrDefault(resourceName, 0.0);
    }

    public Map<String, Double> getArtifactProgressMap() {
        return artifactProgressMap;
    }

    public void resetBytes() {
        downloadedBytes.set(0);
    }

    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public void complete() {
        downloadStatus = Status.COMPLETED;
    }
}
