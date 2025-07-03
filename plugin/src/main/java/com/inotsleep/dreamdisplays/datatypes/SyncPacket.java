package com.inotsleep.dreamdisplays.datatypes;

import java.util.UUID;

/// SyncPacket class represents a synchronization packet used for managing the playback state of displays.
/// It contains information about the display ID, whether it is in sync, the current state of playback, the current time, and the limit time for playback.
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