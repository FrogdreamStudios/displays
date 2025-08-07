package com.inotsleep.dreamdisplays.client.agent;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

public class JarLoader {
    public static void loadLibrariesAtRuntime(List<Path> jarPaths) throws IOException {
        Instrumentation inst = ByteBuddyAgent.install();

        for (Path jar : jarPaths) {
            inst.appendToSystemClassLoaderSearch(new JarFile(jar.toFile()));
        }
    }
}
