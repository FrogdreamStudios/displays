package ru.l0sty.frogdisplays.util;

import me.inotsleep.utils.LoggerFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ImageUtil {

    public static CompletableFuture<NativeImageBackedTexture> fetchImageTextureFromUrl(String url) {
        CompletableFuture<NativeImage> imageFuture = CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage bi = ImageIO.read(URL.of(URI.create(url), null));
                if (bi == null) {
                    throw new IOException("Failed to decode image: " + url);
                }
                return convertToNativeImage(bi);
            } catch (Exception e) {
                LoggerFactory.getLogger().log(Level.SEVERE, "Failed to load image from " + url, e);
                return null;
            }
        });

        return imageFuture.thenCompose(nativeImage -> {
            CompletableFuture<NativeImageBackedTexture> texFuture = new CompletableFuture<>();

            MinecraftClient.getInstance().execute(() -> {
//                RenderSystem.queueFencedTask(() -> {
                    try {
                        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> url, nativeImage);
                        texFuture.complete(tex);
                    } catch (Throwable t) {
                        texFuture.completeExceptionally(t);
                    }
                });
//            });

            return texFuture;
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
