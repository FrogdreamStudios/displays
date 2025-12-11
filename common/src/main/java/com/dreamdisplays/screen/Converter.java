package com.dreamdisplays.screen;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// Native image format converter for high-performance pixel operations.
// Uses native C code when available, Java fallback on all platforms
public class Converter {

    private static boolean useNativeLibrary = false;

    static {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();

            String libName;
            String libExtension;

            // Determine library name and extension based on OS
            if (osName.contains("win")) {
                libName = "dreamdisplays_native";
                libExtension = ".dll";
            } else if (osName.contains("mac")) {
                libName = "libdreamdisplays_native";
                libExtension = ".dylib";
            } else {
                // Linux and other Unix-like systems
                libName = "libdreamdisplays_native";
                libExtension = ".so";
            }

            String fullLibName = libName + libExtension;
            String resourcePath = "/natives/" + fullLibName;

            // Try to load the library from resources
            InputStream libStream = Converter.class.getResourceAsStream(resourcePath);

            if (libStream != null) {
                File tempLib = File.createTempFile("dreamdisplays_native_", libExtension);
                tempLib.deleteOnExit();

                // Copy the library to a temporary file
                Files.copy(libStream, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
                libStream.close();

                // Load the native library
                System.load(tempLib.getAbsolutePath());
                useNativeLibrary = true;
                System.out.println("Dream Displays: Native library loaded (" + osName + "/" + osArch + ")");
            } else {
                System.out.println("Dream Displays: Native library not found for " + osName + ", using Java fallback");
            }
        } catch (Exception e) {
            System.err.println("Dream Displays: Failed to load native library: " + e.getMessage());
            System.out.println("Dream Displays: Falling back to Java implementation");
        }
    }

    // Native method for scaling RGBA images (C implementation)
    private static native void scaleRGBAImage(ByteBuffer src, int srcW, int srcH, ByteBuffer dst, int dstW, int dstH);

    // Java fallback implementation for image scaling
    private static void scaleRGBAImageJava(ByteBuffer src, int srcW, int srcH, ByteBuffer dst, int dstW, int dstH) {
        if (src == null || dst == null) {
            throw new IllegalArgumentException("Source and destination buffers cannot be null");
        }

        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }

        float xRatio = (float) srcW / dstW;
        float yRatio = (float) srcH / dstH;

        for (int y = 0; y < dstH; y++) {
            for (int x = 0; x < dstW; x++) {
                int srcX = (int) (x * xRatio);
                int srcY = (int) (y * yRatio);

                // Clamp coordinates to valid range
                srcX = Math.min(srcX, srcW - 1);
                srcY = Math.min(srcY, srcH - 1);

                // Read pixel from source (RGBA: 4 bytes per pixel)
                int srcIdx = (srcY * srcW + srcX) * 4;
                int pixel = src.getInt(srcIdx);

                // Write pixel to destination
                int dstIdx = (y * dstW + x) * 4;
                dst.putInt(dstIdx, pixel);
            }
        }
    }

    // Public API: Scale RGBA image with automatic native/Java fallback
    public static void scaleRGBA(ByteBuffer src, int srcW, int srcH, ByteBuffer dst, int dstW, int dstH) {
        if (useNativeLibrary) {
            try {
                scaleRGBAImage(src, srcW, srcH, dst, dstW, dstH);
            } catch (UnsatisfiedLinkError e) {
                // Fallback if native method fails
                System.err.println("Dream Displays: Native method call failed, using Java fallback: " + e.getMessage());
                scaleRGBAImageJava(src, srcW, srcH, dst, dstW, dstH);
            }
        } else {
            scaleRGBAImageJava(src, srcW, srcH, dst, dstW, dstH);
        }
    }
}
