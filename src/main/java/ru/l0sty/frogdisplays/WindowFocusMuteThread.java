package ru.l0sty.frogdisplays;

import ru.l0sty.frogdisplays.screen.MediaPlayer;
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


            boolean focused = client.isWindowFocused();

            MediaPlayer.captureSamples = focused || FrogDisplaysMod.getConfig().renderOnAltTab;

            if (FrogDisplaysMod.getConfig().muteOnAltTab) for (Screen screen : ScreenManager.getScreens()) {
                screen.mute(!focused);
            }

            Screen s = ScreenManager.screens.elements().asIterator().next();
            if (s != null) System.out.println(s.muted);

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }



}
