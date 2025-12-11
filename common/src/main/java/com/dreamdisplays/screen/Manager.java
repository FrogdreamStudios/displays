package com.dreamdisplays.screen;

import me.inotsleep.utils.logging.LoggingManager;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public class Manager {

    public static final ConcurrentHashMap<UUID, Screen> screens = new ConcurrentHashMap<>();

    public Manager() {
    }

    public static Collection<Screen> getScreens() {
        return screens.values();
    }

    public static void registerScreen(Screen screen) {
        if (screens.containsKey(screen.getID())) {
            Screen old = screens.get(screen.getID());
            old.unregister();
        }

        screens.put(screen.getID(), screen);

        // Save the display data for persistence
        saveScreenData(screen);
    }

    public static void unregisterScreen(Screen screen) {
        screens.remove(screen.getID());
        screen.unregister();
    }

    public static void unloadAll() {
        for (Screen screen : screens.values()) {
            screen.unregister();
        }

        screens.clear();
    }

    // Save screen data to persistent storage
    public static void saveScreenData(Screen screen) {
        Settings.FullDisplayData data = new Settings.FullDisplayData(
                screen.getID(),
                screen.getPos().getX(),
                screen.getPos().getY(),
                screen.getPos().getZ(),
                screen.getFacing(),
                (int) screen.getWidth(),
                (int) screen.getHeight(),
                screen.getVideoUrl(),
                screen.getLang(),
                (float) screen.getVolume(),
                screen.getQuality(),
                screen.muted,
                screen.isSync,
                screen.getOwnerId(),
                screen.getRenderDistance(),
                screen.getCurrentTimeNanos()
        );

        Settings.saveDisplayData(screen.getID(), data);
    }

    // Load displays from persistent storage for a server
    public static void loadScreensForServer(String serverId) {
        Settings.loadServerDisplays(serverId);

        Collection<Settings.FullDisplayData> allDisplays = Settings.getAllDisplaysForServer();
        for (Settings.FullDisplayData data : allDisplays) {
            if (data.id == null || data.videoUrl == null || data.videoUrl.isEmpty()) {
                LoggingManager.warn("Skipping invalid display data: " + data.id);
                continue;
            }

            Screen screen = new Screen(data.id, data.ownerId, data.x, data.y, data.z,
                    data.facing, data.width, data.height, data.isSync);

            screen.setVolume(data.volume);
            screen.setQuality(data.quality);
            screen.muted = data.muted;
            screen.setSavedTimeNanos(data.currentTimeNanos);
            screen.setRenderDistance(data.renderDistance);

            screen.loadVideo(data.videoUrl, data.lang);

            registerScreen(screen);
            LoggingManager.info("Restored display: " + data.id + " at " + data.x + ", " + data.y + ", " + data.z + " (time: " + (data.currentTimeNanos / 1_000_000_000) + "s)");
        }
    }

    // Save all screens to persistent storage for current server
    public static void saveAllScreens() {
        for (Screen screen : screens.values()) {
            saveScreenData(screen);
        }
        LoggingManager.info("Saved " + screens.size() + " displays");
    }

    // Restore displays that were unloaded but stored in persistent storage
    public static void restoreNearbyDisplays(int playerX, int playerY, int playerZ, double maxDistance) {
        Collection<Settings.FullDisplayData> allDisplays = Settings.getAllDisplaysForServer();
        for (Settings.FullDisplayData data : allDisplays) {
            if (screens.containsKey(data.id)) {
                continue;
            }

            // Use display's saved render distance, fallback to config if not set
            double displayRenderDistance = data.renderDistance > 0 ? data.renderDistance : maxDistance;

            double distance = Math.sqrt(
                    Math.pow(data.x - playerX, 2) +
                            Math.pow(data.y - playerY, 2) +
                            Math.pow(data.z - playerZ, 2)
            );

            if (distance <= displayRenderDistance && data.videoUrl != null && !data.videoUrl.isEmpty()) {
                // Restore this display
                Screen screen = new Screen(data.id, data.ownerId, data.x, data.y, data.z,
                        data.facing, data.width, data.height, data.isSync);

                screen.setVolume(data.volume);
                screen.setQuality(data.quality);
                screen.muted = data.muted;
                screen.setSavedTimeNanos(data.currentTimeNanos);
                screen.setRenderDistance(data.renderDistance);

                // Load the video
                screen.loadVideo(data.videoUrl, data.lang);

                registerScreen(screen);
                LoggingManager.info("Restored nearby display: " + data.id + " at " + data.x + ", " + data.y + ", " + data.z + " (render distance: " + displayRenderDistance + ")");
            }
        }
    }
}
