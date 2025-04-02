package ru.l0sty.frogdisplays.util;

public enum Facing {
    NORTH, EAST, SOUTH, WEST;

    public static Facing fromPacket(byte data) {
        if (data < 0 || data >= values().length)
            throw new IllegalArgumentException("Invalid Facing id: " + data);
        return values()[data];
    }

    public byte toPacket() {
        return (byte) this.ordinal();
    }
}

