package com.inotsleep.dreamdisplays.datatypes;

import java.util.UUID;

/**
 * Represents a synchronization packet for display data.
 * This packet is used to synchronize the state of a display across different clients.
 */
public record SyncPacket(UUID id, boolean isSync, boolean currentState, long currentTime, long limitTime) {
}