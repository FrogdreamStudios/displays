package ru.l0sty.frogdisplays.util;

///  Enum representing the facing direction of a display.
/// This enum is used to determine the orientation of a display in the game world.
/// @return the facing direction of the display.
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