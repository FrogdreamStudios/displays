package ru.l0sty.frogdisplays.util;

import me.inotsleep.utils.logging.LoggingManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Управляет внешним процессом PowerTunnel.
 * TODO: Не работает обход
 */
public class PowerTunnelManager implements AutoCloseable {
    private final String javaExecutable;
    private final String jarPath;
    private final String ip;
    private final int port;
    private Process process;
    private Thread shutdownHook;

    public PowerTunnelManager(String jarPath) {
        this.javaExecutable = "java";
        this.jarPath = jarPath;
        this.ip = "127.0.0.1";
        this.port = 14881;
    }

    /**
     * Запускает PowerTunnel с минимальным набором аргументов:
     * --start    — сразу запускает прокси
     * --console  — консольный режим
     * --ip       — IP-адрес
     * --port     — порт
     */
    public void start() throws IOException {
        File configFile = new File(new File(jarPath).getParentFile(), "configs/libertytunnel.ini");
        configFile.getParentFile().mkdirs();
        List<String> lines = List.of(
                "full_chunking: false",
                "mirror: ",
                "chunk_size: 2",
                "sni_trick: fake",
                "fake_sni: gosuslugi.ru",
                "mix_host_header: true",
                "space_after_get: true",
                "send_payload: false",
                "break_before_get: true",
                "modify_sni: true",
                "enable_chunking: true",
                "mix_host_case: true",
                "dot_after_host: true",
                "generate_pac: false",
                "mirror_interval: interval_2",
                "mix_host_case_complete: true"
        );

        // Создаём файл, если его нет, и перезаписываем содержимое
        Files.write(
                configFile.toPath(),
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        List<String> cmd = List.of(
            javaExecutable,
                "-jar", jarPath,
                "--console",
                "--ip", ip,
                "--port", String.valueOf(port),
                "--start"
                );

        ProcessBuilder pb = new ProcessBuilder(cmd)
            .directory(new File(jarPath).getParentFile())
            .redirectErrorStream(true);

        process = pb.start();

        new Thread(() -> {
            try {
                Thread.sleep(5000);

                Utils.installToCurrentUserRoot(new File(new File(jarPath).getParentFile(), "cert"));
            } catch (Exception e) {
                LoggingManager.error("Unable to load certs", e);
            }
        }).start();

        new Thread(() -> {
            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                    )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // а) просто выводим в свою консоль:
                    LoggingManager.log("[PowerTunnel] " + line);

                    // б) или кидаем в UI-элемент, например, JTextArea:
                    // SwingUtilities.invokeLater(() -> textArea.append(line + "\n"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "PowerTunnel-Output-Reader").start();

        // Hook для гарантированного завершения PowerTunnel при остановке JVM
        shutdownHook = new Thread(this::stop, "PowerTunnel-ShutdownHook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Останавливает процесс (корректно, с таймаутом, затем принудительно).
     */
    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        stop();
        try {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        } catch (IllegalStateException ignored) {
        }
    }
}
