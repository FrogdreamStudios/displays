package com.inotsleep.dreamdisplays.client;

import java.util.UUID;

public interface ClientMod {
    void sendSyncUpdate(UUID id, long time, boolean paused, boolean isSync, long duration);
    void sendRequestSync(UUID id);
    void sendDeletePacket(UUID id);
    void sendReportPacket(UUID id);

    UUID getPlayerID();
    boolean isFocused();
}
