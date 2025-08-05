package com.inotsleep.dreamdisplays.client.display;

import java.awt.image.BufferedImage;

public record DisplayRenderData(BufferedImage image, double x, double y, double z, int width, int height, Display.Facing facing, int textureWidth, int textureHeight) {

}
