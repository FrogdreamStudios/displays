package com.inotsleep.dreamdisplays.client;

import java.util.UUID;

public interface ClientMod {
    void sendSyncUpdate(UUID id, long time, boolean paused, boolean isSync, long duration);
    void sendReport(UUID id);
    void sendVersion(String version);
    UUID getPlayerID();
}
