package com.inotsleep.dreamdisplays.client;

public class ClientModHolder {
    private static ClientMod instance;

    public static ClientMod getInstance() {
        return instance;
    }

    public static void setInstance(ClientMod instance) {
        ClientModHolder.instance = instance;
    }
}
