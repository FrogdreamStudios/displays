package com.inotsleep.dreamdisplays.client.display;

import me.inotsleep.utils.config.Path;
import me.inotsleep.utils.config.SerializableObject;

public class DisplaySettings extends SerializableObject {
    @Path("volume")
    public double volume;

    @Path("quality")
    public int quality;
}
