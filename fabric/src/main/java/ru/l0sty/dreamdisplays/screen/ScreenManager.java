package ru.l0sty.dreamdisplays.screen;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScreenManager is a class that manages all screens in the application.
 */
public class ScreenManager {

    public static final ConcurrentHashMap<UUID, Screen> screens = new ConcurrentHashMap<>();

    public ScreenManager() {
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
}