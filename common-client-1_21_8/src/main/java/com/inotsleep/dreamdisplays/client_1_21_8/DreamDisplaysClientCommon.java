package com.inotsleep.dreamdisplays.client_1_21_8;

import com.inotsleep.dreamdisplays.client.DisplayManager;
import com.inotsleep.dreamdisplays.client.display.Display;
import com.inotsleep.dreamdisplays.client_1_21_8.packets.DeletePacket;
import com.inotsleep.dreamdisplays.client_1_21_8.packets.DisplayInfoPacket;
import com.inotsleep.dreamdisplays.client_1_21_8.packets.PremiumPacket;
import com.inotsleep.dreamdisplays.client_1_21_8.packets.SyncPacket;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.Minecraft;

public class DreamDisplaysClientCommon {
    public static final String MOD_ID = "dreamdisplays";

    public static void onModInit() {
        LoggingManager.info("Hello from common module!");
        LoggingManager.info(Minecraft.getInstance().toString());

    }

    public static void onDisplayInfoPacket(DisplayInfoPacket payload) {
        Display display = DisplayManager.getDisplay(payload.id());
        if (display != null) {
            display.setVideoCode(payload.videoCode());
            display.setLanguage(payload.lang());
            display.setSync(payload.isSync());
        }
    }

    public static void onPremiumPacket(PremiumPacket payload) {

    }

    public static void onDeletePacket(DeletePacket deletePacket) {

    }

    public static void onSyncPacket(SyncPacket payload) {

    }
}
