package com.inotsleep.dreamdisplays.client;

import com.inotsleep.dreamdisplays.client.display.Display;
import com.inotsleep.dreamdisplays.client.display.DisplaySettings;
import com.inotsleep.dreamdisplays.client.utils.Utils;
import me.inotsleep.utils.config.AbstractConfig;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class DisplayManager {
    private static final Map<UUID, Display> displays = new HashMap<>();
    private static WorldDisplaySettingsStorage displaySettings;
    private static Path savePath;

    private static final Map<Class<? extends AutoCloseable>, Map<UUID, ? extends Closeable>> dataHolder = new HashMap<>();

    public static Collection<Display> getDisplays() {
        return displays.values();
    }

    public static Display getDisplay(UUID uuid) {
        return displays.get(uuid);
    }

    public static void addDisplay(Display display) {
        Display old = displays.get(display.getId());
        if (old != null) {
            old.close();
        }

        displays.put(display.getId(), display);

        if (displaySettings != null) {
            DisplaySettings settings = displaySettings.settings.get(display.getId().toString());
            if (settings != null) {
                display.applySettings(settings);
            }
        }
    }

    public static void removeDisplay(UUID id) {
        Display display = displays.get(id);
        if (display == null) return;

        display.close();
        displays.remove(id);
        dataHolder.values().forEach(map -> Utils.closeWithExceptionHandle(map.remove(id)));
    }

    public static void setSavePath(Path path) {
        if (savePath != null) {
            if (savePath.equals(path)) return;
        }

        if (path == null) return;

        savePath = path;

        displaySettings = new WorldDisplaySettingsStorage(path.toFile());
        displaySettings.reload();
    }

    public static void saveSettings(Display display) {
        if (displaySettings == null) return;
        DisplaySettings settings = displaySettings.settings.computeIfAbsent(display.getId().toString(), k -> new DisplaySettings());
        settings.volume = display.getVolume();
        settings.quality = display.getQuality();
    }

    public static void saveSettings() {
        if (displaySettings != null) displaySettings.save();
    }

    public static void mute(boolean unfocused) {
        displays.forEach((uuid, display) -> display.mute(unfocused));
    }

    public static void doRender(boolean focused) {
        displays.forEach((uuid, display) -> display.doRender(focused));
    }

    public static void closeDisplays() {
        displays.forEach((uuid, display) -> display.close());

        displays.clear();
        dataHolder.values().forEach(map -> {
            map.values().forEach(Utils::closeWithExceptionHandle);
            map.clear();
        });

        dataHolder.clear();
    }

    private static class WorldDisplaySettingsStorage extends AbstractConfig {
        @me.inotsleep.utils.config.Path("displays")
        Map<String, DisplaySettings> settings = new HashMap<>();

        public WorldDisplaySettingsStorage(File configFile) {
            super(configFile);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AutoCloseable> T getData(Class<T> clazz, UUID id) {
        Map<UUID, T> data = (Map<UUID, T>) dataHolder.computeIfAbsent(clazz, k -> new HashMap<>());

        return data.get(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AutoCloseable> void setData(Class<T> clazz, UUID id, T object) {
        Map<UUID, T> data = (Map<UUID, T>) dataHolder.computeIfAbsent(clazz, k -> new HashMap<>());
        data.put(id, object);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AutoCloseable> void removeData(Class<T> clazz, UUID id) {
        Map<UUID, T> data = (Map<UUID, T>) dataHolder.computeIfAbsent(clazz, k -> new HashMap<>());
        Utils.closeWithExceptionHandle(data.remove(id));
    }
}
