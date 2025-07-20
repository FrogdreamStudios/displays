package com.inotsleep.agent;

import net.bytebuddy.agent.Attacher;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class JarLoader {
    public static void loadLibrariesAtRuntime(String... jarPaths) throws Exception {
        Instrumentation inst = ByteBuddyAgent.install();

        for (String jar : jarPaths) {
            inst.appendToSystemClassLoaderSearch(new JarFile(jar));
        }
    }
}
