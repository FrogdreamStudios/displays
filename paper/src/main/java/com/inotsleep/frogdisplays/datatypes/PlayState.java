package com.inotsleep.frogdisplays.datatypes;

import com.inotsleep.frogdisplays.managers.DisplayManager;

import java.util.UUID;

/**
 * PlayState class represents the state of a display playback.
 * It holds information about whether the playback is paused, the last reported time, and the limit time for the playback.
 */
public class PlayState {
    private final UUID id;
    private boolean paused = false;
    private long lastReportedTime = 0;
    private long lastReportedTimeTimestamp = 0;
    private long limitTime = 0;
    public DisplayData displayData;

    public PlayState (UUID id) {
        this.id = id;
        displayData = DisplayManager.getDisplayData(id);
    }

    public void update(SyncPacket packet) {
        this.paused = packet.getCurrentState();
        this.lastReportedTime = packet.getCurrentTime();
        this.lastReportedTimeTimestamp = System.nanoTime();
        limitTime = packet.getLimitTime();
    }

    public SyncPacket createPacket() {
        long nanos = System.nanoTime();
        long currentTime;

        if (paused) {
            currentTime = lastReportedTime;
        } else {
            long elapsed = nanos - lastReportedTimeTimestamp;
            currentTime = lastReportedTime + elapsed;
        }

        if (limitTime == 0 && displayData.getDuration() != null) {
            limitTime = displayData.getDuration();
        }

        if (limitTime > 0) {
            currentTime %= limitTime;
        }

        return new SyncPacket(id, true, paused, currentTime, limitTime);
    }
}