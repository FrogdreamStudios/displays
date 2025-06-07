package ru.l0sty.frogdisplays.util;

import me.inotsleep.utils.logging.LoggingManager;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "macos";
        } else if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            return "linux";
        }
        throw new UnsupportedOperationException("Unsupported OS: " + os);
    }

    public static String extractVideoId(String youtubeUrl) {
        try {
            URI uri = new URI(youtubeUrl);
            String query = uri.getQuery();                // берёт часть после "?"
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && pair[0].equals("v")) {
                        return pair[1];
                    }
                }
            }
            // если короткая ссылка youtu.be/ID
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

    public static String readResource(String resourcePath) throws IOException {
        try (InputStream in = Utils.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Ресурс не найден: " + resourcePath);
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

    /**
     * Возвращает true, если в CurrentUser\Root уже есть сертификат с таким Subject.
     */
    private static boolean isInstalled(File certFile) throws Exception {
        // читаем Subject из certFile
        String subject;
        try (InputStream is = Files.newInputStream(certFile.toPath())) {
            X509Certificate cert = (X509Certificate)
                    CertificateFactory.getInstance("X.509").generateCertificate(is);
            subject = cert.getSubjectX500Principal().getName();
        }
        // запускаем certutil и парсим вывод
        ProcessBuilder pb = new ProcessBuilder("certutil", "-store", "-user", "Root");
        Process p = pb.start();
        String all = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        return all.contains(subject);
    }


    public static void installToCurrentUserRoot(File certDir) throws Exception {
        if (!certDir.exists() || !certDir.isDirectory()) {
            throw new IllegalArgumentException("Ожидалась директория с сертификатами, а передано: "
                    + certDir.getAbsolutePath());
        }

        File[] cerFiles = certDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".cer"));
        if (cerFiles == null || cerFiles.length == 0) {
            LoggingManager.warn("В папке нет .cer-файлов: " + certDir.getAbsolutePath());
            return;
        }

        for (File cert : cerFiles) {
            LoggingManager.info("Installing " + cert.getAbsolutePath());

            if (isInstalled(cert)) {
                LoggingManager.info("Cert already installed: " + cert.getAbsolutePath());
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "certutil",
                    "-addstore",
                    "-user",      // чтобы писать в хранилище текущего пользователя
                    "Root",       // Trusted Root Certification Authorities
                    cert.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            try (InputStream is = proc.getInputStream()) {
                // нужно читать поток, чтобы certutil не блокировался
                is.transferTo(System.out);
            }

            int code = proc.waitFor();
            if (code != 0) {
                throw new RuntimeException(
                        "Ошибка при добавлении сертификата " +
                                cert.getName() + ", код возврата: " + code
                );
            }
        }

        LoggingManager.info("Все найденные .cer-файлы установлены.");
    }
}
