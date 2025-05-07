package ru.l0sty.frogdisplays;

import ru.l0sty.frogdisplays.screen.Screen;
import net.minecraft.client.MinecraftClient;
import ru.l0sty.frogdisplays.screen.ScreenManager;

public class WindowFocusMuteThread extends Thread {
    public static WindowFocusMuteThread instance;

    public WindowFocusMuteThread() {
        setDaemon(true);
        instance = this;
        setName("window-focus-mute-thread");
    }

    @Override
    public void run() {
        while (true) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                break;
            }

            boolean focused = client.isWindowFocused();

            if (PlatformlessInitializer.getConfig().muteOnAltTab) for (Screen screen : ScreenManager.getScreens()) {
                screen.mute(!focused);
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }



}
