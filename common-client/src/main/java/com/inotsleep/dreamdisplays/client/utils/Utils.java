package com.inotsleep.dreamdisplays.client.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
}
