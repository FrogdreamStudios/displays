package com.inotsleep.frogdisplays.datatypes;

import java.util.UUID;

/**
 * Represents a synchronization packet for display data.
 * This packet is used to synchronize the state of a display across different clients.
 */
public class SyncPacket {
    private final UUID   id;
    private final boolean isSync;
    private final boolean currentState;
    private final long    currentTime;
    private final long    limitTime;

    public SyncPacket(UUID id, boolean isSync, boolean currentState, long currentTime, long limitTime) {
        this.id = id;
        this.isSync = isSync;
        this.currentState = currentState;
        this.currentTime = currentTime;
        this.limitTime = limitTime;
    }

    public UUID getId() {
        return id;
    }

    public boolean isSync() {
        return isSync;
    }

    public boolean getCurrentState() {
        return currentState;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public long getLimitTime() {
        return limitTime;
    }
}