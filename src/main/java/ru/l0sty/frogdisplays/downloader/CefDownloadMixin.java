/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2023 CinemaMod Group
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package ru.l0sty.frogdisplays.downloader;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import me.inotsleep.utils.LoggerFactory;
import oshi.jna.platform.unix.CLibrary;
import oshi.jna.platform.windows.Kernel32;
import ru.l0sty.frogdisplays.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;


public class CefDownloadMixin {
    private static void setupLibraryPath() throws IOException {
        final File gStreamerLibrariesDir = new File("./libs/gstreamer");
// Disabled as not working :(
//        if (!gStreamerLibrariesDir.exists()) gStreamerLibrariesDir.mkdirs();
//        System.setProperty("jna.library.path", String.join(File.pathSeparator, new File(gStreamerLibrariesDir, "bin").getCanonicalPath()/*, new File(gStreamerLibrariesDir, "lib")
//                new File("C:/Program Files/gstreamer/1.0/mingw_x86_64/bin").getCanonicalPath()*/));
//        System.setProperty("GST_PLUGIN_SYSTEM_PATH",
//                new File(gStreamerLibrariesDir, "lib/gstreamer-1.0").getCanonicalPath());
//        System.setProperty("GST_PLUGIN_PATH",
//                new File(gStreamerLibrariesDir, "lib/gstreamer-1.0").getCanonicalPath());

    }

    public static void sinit() {
        try {
            setupLibraryPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread downloadThread = new Thread(() -> {
            GStreamerDownloader downloader = new GStreamerDownloader();
            boolean downloadGStreamer;

            try {
                downloadGStreamer = !downloader.downloadGstreamerChecksum();
            } catch (IOException e) {
                LoggerFactory.getLogger().log(Level.SEVERE, "Failed to download JCEF checksum.", e);
                GStreamerDownloadListener.INSTANCE.setFailed(true);
                return;
            }

            // Ensure the mcef-libraries directory exists
            // If not, we want to try redownloading
            File mcefLibrariesDir = new File("./libs/gstreamer");
            downloadGStreamer |= !mcefLibrariesDir.exists();

            if (downloadGStreamer) {
                try {
                    downloader.downloadGstreamerBuild();
                } catch (IOException e) {
                    LoggerFactory.getLogger().log(Level.SEVERE,"Failed to download JCEF.", e);
                    GStreamerDownloadListener.INSTANCE.setFailed(true);
                    return;
                }

                downloader.extractGstreamer(true);
            }

            GStreamerDownloadListener.INSTANCE.setDone(true);
        });
        downloadThread.start();
    }
}