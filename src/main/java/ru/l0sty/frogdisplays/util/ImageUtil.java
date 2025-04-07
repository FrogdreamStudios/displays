package ru.l0sty.frogdisplays.util;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public final class ImageUtil {

    public static CompletableFuture<NativeImageBackedTexture> fetchImageTextureFromUrl(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage bufferedImage = ImageIO.read(new URL(url));
                if (bufferedImage == null) throw new IOException("Failed to decode image: " + url);

                NativeImage nativeImage = convertToNativeImage(bufferedImage);
                return new NativeImageBackedTexture(nativeImage);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private static NativeImage convertToNativeImage(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int rgba = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setColor(x, y, rgba);

            }
        }

        return nativeImage;
    }


}
