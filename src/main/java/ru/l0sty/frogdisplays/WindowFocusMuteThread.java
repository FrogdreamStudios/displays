package ru.l0sty.frogdisplays;

import ru.l0sty.frogdisplays.screen.Screen;
import net.minecraft.client.MinecraftClient;
import ru.l0sty.frogdisplays.screen.ScreenManager;

public class WindowFocusMuteThread extends Thread {

    public WindowFocusMuteThread() {
        setDaemon(true);
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

            if (FrogDisplaysMod.getConfig().muteOnAltTab) {
                boolean focused = client.isWindowFocused();

                // При первом заходе инициализируем previousState,
                // а при смене состояния фокуса — переключаем mute
                if (focused != previousState) {
                    for (Screen screen : ScreenManager.getScreens()) {
                        screen.mute(!focused);
                    }
                    previousState = focused;
                }
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
