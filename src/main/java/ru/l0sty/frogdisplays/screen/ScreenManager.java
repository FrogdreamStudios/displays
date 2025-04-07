package ru.l0sty.frogdisplays.screen;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScreenManager {

    private final ConcurrentHashMap<UUID, Screen> screens;

    public ScreenManager() {
        screens = new ConcurrentHashMap<>();
    }

    public Collection<Screen> getScreens() {
        return screens.values();
    }

    public void registerScreen(Screen screen) {
        if (screens.containsKey(screen.getID())) {
            Screen old = screens.get(screen.getID());
            old.unregister();
            old.closeBrowser();
        }

        screens.put(screen.getID(), screen);
    }

    public void unregisterScreen(Screen screen) {
        screens.remove(screen.getID());
        screen.closeBrowser();
        screen.unregister();
    }

    public boolean hasActiveScreen() {
        for (Screen screen : screens.values()) {
            if (screen.hasBrowser()) {
                return true;
            }
        }

        return false;
    }

    public void unloadAll() {
        for (Screen screen : screens.values()) {
            screen.closeBrowser();
            screen.unregister();
        }

        screens.clear();
    }



}
