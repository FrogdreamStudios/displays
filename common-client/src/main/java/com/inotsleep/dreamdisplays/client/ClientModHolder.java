package com.inotsleep.dreamdisplays.client;

import java.util.concurrent.locks.LockSupport;

public class ClientModHolder {
    private static Thread windowFocusThread;

    private static ClientMod instance;

    public static ClientMod getInstance() {
        return instance;
    }

    public static void initialize(ClientMod instance) {
        ClientModHolder.instance = instance;

        windowFocusThread = new Thread(() -> {
            LockSupport.parkNanos(10_000_000);
            if (Config.getInstance().muteOnLostFocus) {
                DisplayManager.mute(!instance.isFocused());
            }

            if (Config.getInstance().noRenderOnLostFocus) {
                DisplayManager.doRender(instance.isFocused());
            }
        }, "Window focus thread");

        windowFocusThread.start();
    }
}
