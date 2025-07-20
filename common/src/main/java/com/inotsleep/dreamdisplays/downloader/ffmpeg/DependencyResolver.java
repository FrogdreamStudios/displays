package com.inotsleep.dreamdisplays.downloader.ffmpeg;

import com.inotsleep.dreamdisplays.downloader.DependencyConfig;
import com.inotsleep.dreamdisplays.downloader.Utils;
import com.inotsleep.dreamdisplays.downloader.maven.MavenResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyResolver {
    private static final DependencyFilter filter = DependencyFilterUtils.andFilter(
            (node, parents) -> {
                Artifact art = node.getArtifact();
                if (art == null) return false;
                String cls = art.getClassifier();
                return (cls == null || cls.isEmpty() || cls.equals(Utils.getPlatform()));
            },
            new ScopeDependencyFilter(List.of("compile"), List.of())
    );

    public static List<Path> resolve(MavenResolver resolver, DependencyConfig config) throws DependencyResolutionException {
        CollectRequest collectRequest = createCollectRequest(resolver, config);

        DependencyRequest depRequest = new DependencyRequest(collectRequest, filter);
        DependencyResult  depResult  = resolver.getSystem().resolveDependencies(resolver.getSession(), depRequest);

        return depResult.getArtifactResults().stream().map(ArtifactResult::getArtifact).map(Artifact::getPath).toList();
    }

    public static Map<String, Long> getEstimatedSize(MavenResolver resolver, DependencyConfig config) throws DependencyCollectionException {
        CollectRequest collectRequest = createCollectRequest(resolver, config);

        CollectResult collectResult = resolver.getSystem()
                .collectDependencies(resolver.getSession(), collectRequest);
        DependencyNode rootNode = collectResult.getRoot();

        List<Artifact> filtered = new ArrayList<>();
        collectFiltered(rootNode, filtered);

        Map<String, Long> sizes = new HashMap<>();

        filtered.forEach(artifact -> sizes.put(getCoords(artifact), estimateArtifactSize(resolver, artifact)));

        return sizes;
    }

    private static CollectRequest createCollectRequest(MavenResolver resolver, DependencyConfig config) {
        CollectRequest collectRequest = new CollectRequest();
        for (String coords : config.dependencies) {
            collectRequest.addDependency(new Dependency(new DefaultArtifact(coords), "compile"));
        }
        collectRequest.setRepositories(resolver.getRemoteRepositories());

        return collectRequest;
    }

    public static void collectFiltered(DependencyNode node, List<Artifact> out
    ) {
        if (filter.accept(node, List.of())) {
            Artifact art = node.getArtifact();
            if (art != null) {
                out.add(art);
            }
        }
        for (DependencyNode child : node.getChildren()) {
            collectFiltered(child, out);
        }
    }

    private static String getCoords(Artifact artifact) {
        String artifactId = artifact.getArtifactId();
        String version    = artifact.getVersion();
        String ext        = artifact.getExtension();
        String classifier = artifact.getClassifier();

        return artifactId + "-" + version
                + (classifier != null && !classifier.isEmpty() ? "-" + classifier : "")
                + "." + ext;
    }

    private static long estimateArtifactSize(MavenResolver resolver, Artifact artifact) {
        String artifactId = artifact.getArtifactId();
        String version    = artifact.getVersion();
        String groupPath = artifact.getGroupId().replace('.', '/');
        String fileName   = getCoords(artifact);

        for (RemoteRepository repo : resolver.getRemoteRepositories()) {
            try {
                String baseUrl = repo.getUrl();
                if (!baseUrl.endsWith("/")) baseUrl += "/";
                String artifactPath = String.join("/",
                        groupPath,
                        artifactId,
                        version,
                        fileName
                );
                URL url = URI.create(baseUrl + artifactPath).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.connect();
                long size = conn.getContentLengthLong();
                conn.disconnect();
                if (size >= 0) {
                    return size;
                }
            } catch (IOException ignored) {

            }
        }
        return -1L;
    }
}