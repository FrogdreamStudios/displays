package com.inotsleep.dreamdisplays.client.utils;

import me.inotsleep.utils.logging.LoggingManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String getPlatform() {
        String os   = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (os.contains("win")) {
            return arch.contains("64") || arch.equals("amd64")
                    ? "windows-x86_64" : "windows-x86";
        } else if (os.contains("mac")) {
            return arch.equals("aarch64")
                    ? "macosx-arm64" : "macosx-x86_64";
        } else if (os.contains("nux")) {
            if (arch.equals("aarch64")) {
                return "linux-arm64";
            } else if (arch.equals("ppc64le")) {
                return "linux-ppc64le";
            } else {
                return "linux-x86_64";
            }
        }
        throw new IllegalStateException("Unsupported OS/arch: " + os + "/" + arch);
    }

    public static String readResource(String resourcePath) throws IOException {
        try (InputStream in = Utils.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Can't find the resource: " + resourcePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    public static void closeWithExceptionHandle(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            LoggingManager.error("Unable to close object", e);
        }
    }

    public static String extractVideoId(String youtubeUrl) {
        try {
            URI uri = new URI(youtubeUrl);
            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && pair[0].equals("v")) {
                        return pair[1];
                    }
                }
            }

            // If the URL is a shortened version or a YouTube Shorts link
            String host = uri.getHost();
            if (host != null && host.contains("youtu.be")) {
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    return path.substring(1);
                }
            } else if (host != null && host.contains("youtube.com")) {
                String path = uri.getPath();
                if (path != null && path.contains("shorts")) {
                    return List.of(path.split("/")).getLast();
                }
            }
        } catch (URISyntaxException ignored) {
        }

        String regex = "(?<=([?&]v=))[^#&?]*";
        Matcher m = Pattern.compile(regex).matcher(youtubeUrl);
        return m.find() ? m.group() : null;
    }
}
