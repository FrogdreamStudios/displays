package ru.l0sty.frogdisplays;

import ru.l0sty.frogdisplays.screen.MediaPlayer;
import ru.l0sty.frogdisplays.screen.Screen;
import net.minecraft.client.MinecraftClient;
import ru.l0sty.frogdisplays.screen.ScreenManager;

public class WindowFocusMuteThread extends Thread {
    public static WindowFocusMuteThread instance;

    public WindowFocusMuteThread() {
        setDaemon(true);
        instance = this;
        setName("window-focus-cef-mute-thread");
    }

    @Override
    public void run() {
        boolean previousState = true;
        while (true) {
            // Всегда получаем актуальный инстанс клиента
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                break;
            }

            boolean focused = client.isWindowFocused();

            if (FrogDisplaysMod.getConfig().muteOnAltTab) for (Screen screen : ScreenManager.getScreens()) {
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
