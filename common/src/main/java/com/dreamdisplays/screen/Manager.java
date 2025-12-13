package com.dreamdisplays.screen;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.inotsleep.utils.logging.LoggingManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Manager {

    public static final ConcurrentHashMap<UUID, Screen> screens =
        new ConcurrentHashMap<>();

    public Manager() {}

    public static Collection<Screen> getScreens() {
        return screens.values();
    }

    public static void registerScreen(Screen screen) {
        if (screens.containsKey(screen.getID())) {
            Screen old = screens.get(screen.getID());
            old.unregister();
        }

        Settings.DisplaySettings clientSettings = Settings.getSettings(
            screen.getID()
        );
        screen.setVolume(clientSettings.volume);
        screen.setQuality(clientSettings.quality);
        screen.muted = clientSettings.muted;

        Settings.FullDisplayData savedData = Settings.getDisplayData(
            screen.getID()
        );
        if (savedData != null) {
            screen.setRenderDistance(savedData.renderDistance);
            screen.setSavedTimeNanos(savedData.currentTimeNanos);
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
    // Actual display data comes from the server via Info packets.
    // Local cache is used only for client preferences (volume, quality, muted).
    public static void loadScreensForServer(String serverId) {
        Settings.loadServerDisplays(serverId);
        LoggingManager.info(
            "Initialized display settings storage for server: " + serverId
        );
        // Displays will be received from server via Info packets
    }

    // Save all screens to persistent storage for current server
    public static void saveAllScreens() {
        for (Screen screen : screens.values()) {
            saveScreenData(screen);
        }
        LoggingManager.info("Saved " + screens.size() + " displays");
    }
}
