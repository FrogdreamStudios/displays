package com.dreamdisplays.screen;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// Native image format converter for high-performance pixel operations.
// Converts RGBA/ABGR formats using native code
public class Converter {

    static {
        try {
            // Determine OS and architecture
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            String libName;
            String libExtension;

            if (osName.contains("win")) {
                libName = "dreamdisplays_native";
                libExtension = ".dll";
            } else if (osName.contains("mac")) {
                libName = "libdreamdisplays_native";
                libExtension = ".dylib";
            } else {
                libName = "libdreamdisplays_native";
                libExtension = ".so";
            }

            String fullLibName = libName + libExtension;
            String resourcePath = "/natives/" + fullLibName;

            // Load the library from resources
            InputStream libStream = Converter.class.getResourceAsStream(resourcePath);

            if (libStream != null) {
                File tempLib = File.createTempFile("dreamdisplays_native_", libExtension);
                tempLib.deleteOnExit();

                // Copy the library to a temporary file
                Files.copy(libStream, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
                libStream.close();

                // Load the native library
                System.load(tempLib.getAbsolutePath());
                System.out.println("Dream Displays: Native library loaded (" + osName + "/" + osArch + ")");
            } else {
                System.out.println("Dream Displays: Native library not found, using Java fallback");
            }
        } catch (Exception e) {
            System.err.println("Dream Displays: Failed to load native library: " + e.getMessage());
        }
    }

    // Scale RGBA image using nearest neighbor
    private static native void scaleRGBAImage(ByteBuffer src, int srcW, int srcH, ByteBuffer dst, int dstW, int dstH);

    // Scale RGBA image using nearest neighbor scaling
    public static void scaleRGBA(ByteBuffer src, int srcW, int srcH, ByteBuffer dst, int dstW, int dstH) {
        scaleRGBAImage(src, srcW, srcH, dst, dstW, dstH);
    }
}
