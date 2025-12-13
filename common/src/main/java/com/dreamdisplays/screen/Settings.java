package com.dreamdisplays.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Settings {

    // Store settings per server
    private static final File SETTINGS_DIR = new File("./config/dreamdisplays");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    // displayId -> DisplaySettings
    private static final Map<UUID, DisplaySettings> displaySettings =
        new HashMap<>();
    // serverId -> (displayId -> FullDisplayData)
    private static final Map<
        String,
        Map<UUID, FullDisplayData>
    > serverDisplays = new HashMap<>();
    private static @Nullable String currentServerId = null;

    // Load settings from disk
    public static void load() {
        // Ensure directory exists
        if (!SETTINGS_DIR.exists() && !SETTINGS_DIR.mkdirs()) {
            LoggingManager.error("Failed to create settings directory.");
            return;
        }

        // Load client display settings (volume, quality, muted)
        File clientSettingsFile = new File(
            SETTINGS_DIR,
            "client-display-settings.json"
        );
        try (Reader reader = new FileReader(clientSettingsFile)) {
            Type type = new TypeToken<
                Map<String, DisplaySettings>
            >() {}.getType();
            Map<String, DisplaySettings> loadedSettings = GSON.fromJson(
                reader,
                type
            );

            if (loadedSettings != null) {
                displaySettings.clear();
                for (Map.Entry<
                    String,
                    DisplaySettings
                > entry : loadedSettings.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        displaySettings.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        LoggingManager.error(
                            "Invalid UUID in client display settings: " +
                                entry.getKey()
                        );
                    }
                }
            }
        } catch (FileNotFoundException ignored) {
            // File doesn't exist yet, that's fine
        } catch (IOException e) {
            LoggingManager.error("Failed to load client display settings", e);
        }
    }

    // Load server-specific display data
    public static void loadServerDisplays(String serverId) {
        currentServerId = serverId;
        File serverFile = new File(
            SETTINGS_DIR,
            "server-" + serverId + "-displays.json"
        );

        try (Reader reader = new FileReader(serverFile)) {
            Type type = new TypeToken<
                Map<String, FullDisplayData>
            >() {}.getType();
            Map<String, FullDisplayData> loadedDisplays = GSON.fromJson(
                reader,
                type
            );

            Map<UUID, FullDisplayData> displays = new HashMap<>();
            if (loadedDisplays != null) {
                for (Map.Entry<
                    String,
                    FullDisplayData
                > entry : loadedDisplays.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        displays.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        LoggingManager.error(
                            "Invalid UUID in server displays: " + entry.getKey()
                        );
                    }
                }
            }
            serverDisplays.put(serverId, displays);
            LoggingManager.info(
                "Loaded " +
                    displays.size() +
                    " displays for server: " +
                    serverId
            );
        } catch (FileNotFoundException ignored) {
            serverDisplays.put(serverId, new HashMap<>());
        } catch (IOException e) {
            LoggingManager.error(
                "Failed to load server displays for " + serverId,
                e
            );
            serverDisplays.put(serverId, new HashMap<>());
        }
    }

    // Save all client settings to disk
    public static void save() {
        try {
            if (!SETTINGS_DIR.exists() && !SETTINGS_DIR.mkdirs()) {
                LoggingManager.error("Failed to create settings directory.");
                return;
            }

            Map<String, DisplaySettings> toSave = new HashMap<>();
            for (Map.Entry<
                UUID,
                DisplaySettings
            > entry : displaySettings.entrySet()) {
                toSave.put(entry.getKey().toString(), entry.getValue());
            }

            File clientSettingsFile = new File(
                SETTINGS_DIR,
                "client-display-settings.json"
            );
            try (Writer writer = new FileWriter(clientSettingsFile)) {
                GSON.toJson(toSave, writer);
            }
        } catch (IOException e) {
            LoggingManager.error("Failed to save client display settings", e);
        }
    }

    // Save server-specific displays to disk
    public static void saveServerDisplays(String serverId) {
        try {
            if (!SETTINGS_DIR.exists() && !SETTINGS_DIR.mkdirs()) {
                LoggingManager.error("Failed to create settings directory.");
                return;
            }

            Map<UUID, FullDisplayData> displays = serverDisplays.getOrDefault(
                serverId,
                new HashMap<>()
            );
            Map<String, FullDisplayData> toSave = new HashMap<>();
            for (Map.Entry<UUID, FullDisplayData> entry : displays.entrySet()) {
                toSave.put(entry.getKey().toString(), entry.getValue());
            }

            File serverFile = new File(
                SETTINGS_DIR,
                "server-" + serverId + "-displays.json"
            );
            try (Writer writer = new FileWriter(serverFile)) {
                GSON.toJson(toSave, writer);
            }
            LoggingManager.info(
                "Saved " + toSave.size() + " displays for server: " + serverId
            );
        } catch (IOException e) {
            LoggingManager.error(
                "Failed to save server displays for " + serverId,
                e
            );
        }
    }

    // Get client display settings
    public static DisplaySettings getSettings(UUID displayId) {
        return displaySettings.computeIfAbsent(displayId, k ->
            new DisplaySettings()
        );
    }

    // Update client display settings
    public static void updateSettings(
        UUID displayId,
        float volume,
        String quality,
        boolean muted
    ) {
        DisplaySettings settings = getSettings(displayId);
        settings.volume = volume;
        settings.quality = quality;
        settings.muted = muted;
        save();
    }

    // Get full display data for a server
    public static @Nullable FullDisplayData getDisplayData(UUID displayId) {
        if (currentServerId == null) return null;
        return serverDisplays
            .getOrDefault(currentServerId, new HashMap<>())
            .get(displayId);
    }

    // Save full display data
    public static void saveDisplayData(UUID displayId, FullDisplayData data) {
        if (currentServerId == null) return;

        serverDisplays
            .computeIfAbsent(currentServerId, k -> new HashMap<>())
            .put(displayId, data);
        saveServerDisplays(currentServerId);
    }

    // Remove display from all servers and client settings
    public static void removeDisplay(UUID displayId) {
        // Remove from server-specific display data
        for (Map<UUID, FullDisplayData> displays : serverDisplays.values()) {
            if (displays.remove(displayId) != null) {
                String serverId = null;
                for (Map.Entry<
                    String,
                    Map<UUID, FullDisplayData>
                > entry : serverDisplays.entrySet()) {
                    if (entry.getValue() == displays) {
                        serverId = entry.getKey();
                        break;
                    }
                }
                if (serverId != null) {
                    saveServerDisplays(serverId);
                }
            }
        }

        // Also remove from client display settings (volume, quality, muted)
        displaySettings.remove(displayId);
        save();

        LoggingManager.info(
            "Removed display from all saved data: " + displayId
        );
    }

    // Client settings for a display (volume, quality, muted)
    public static class DisplaySettings {

        public float volume = 0.5f;
        public String quality = "720";
        public boolean muted = false;

        public DisplaySettings() {}
    }

    // Full display data (stored per server)
    public static class FullDisplayData {

        public UUID id;
        public int x;
        public int y;
        public int z;
        public String facing;
        public int width;
        public int height;
        public String videoUrl;
        public String lang;
        public float volume;
        public String quality;
        public boolean muted;
        public boolean isSync;
        public UUID ownerId;
        public int renderDistance = 64;
        public long currentTimeNanos = 0;

        public FullDisplayData(
            UUID id,
            int x,
            int y,
            int z,
            String facing,
            int width,
            int height,
            String videoUrl,
            String lang,
            float volume,
            String quality,
            boolean muted,
            boolean isSync,
            UUID ownerId
        ) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.facing = facing;
            this.width = width;
            this.height = height;
            this.videoUrl = videoUrl;
            this.lang = lang;
            this.volume = volume;
            this.quality = quality;
            this.muted = muted;
            this.isSync = isSync;
            this.ownerId = ownerId;
        }

        public FullDisplayData(
            UUID id,
            int x,
            int y,
            int z,
            String facing,
            int width,
            int height,
            String videoUrl,
            String lang,
            float volume,
            String quality,
            boolean muted,
            boolean isSync,
            UUID ownerId,
            int renderDistance,
            long currentTimeNanos
        ) {
            this(
                id,
                x,
                y,
                z,
                facing,
                width,
                height,
                videoUrl,
                lang,
                volume,
                quality,
                muted,
                isSync,
                ownerId
            );
            this.renderDistance = renderDistance;
            this.currentTimeNanos = currentTimeNanos;
        }
    }
}
