package ru.l0sty.frogdisplays.util;

/**
 * The Facing enum represents the four cardinal compass directions: NORTH, EAST, SOUTH, and WEST.
 * It provides utility methods for converting between enum constants and their respective byte representations,
 * allowing easy serialization and deserialization for use in packet-based communication.
 */
public enum Facing {
    NORTH, EAST, SOUTH, WEST;

    public static Facing fromPacket(byte data) {
        if (data < 0 || data >= values().length)
            throw new IllegalArgumentException("Invalid facing ID: " + data);
        return values()[data];
    }

    public byte toPacket() {
        return (byte) this.ordinal();
    }
}