plugins {
    id("net.neoforged.moddev") version libs.versions.moddev
}

dependencies {
    api(libs.gst1)
    api(libs.utils)
    api(libs.javatube)
    api(libs.jspecify)
}

neoForge {
    enable {
        version = libs.versions.neoforge.get()
    }
}
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
}

// Task to verify native libraries
tasks.register("verifyNatives") {
    doLast {
        val nativesDir = file("src/main/resources/natives")
        if (nativesDir.exists() && nativesDir.isDirectory) {
            val natives = nativesDir.listFiles()?.filter {
                it.name.matches(Regex(".*\\.(so|dylib|dll)$"))
            } ?: emptyList()

            if (natives.isNotEmpty()) {
                println("✓ Native libraries found (${natives.size}):")
                natives.forEach { println("  - ${it.name} (${it.length() / 1024}KB)") }
            } else {
                println("⚠ No native libraries found, will use Java fallback")
            }
        } else {
            println("⚠ Native libraries directory not found, will use Java fallback")
        }
    }
}

// Ensure verification runs before JAR creation
tasks.jar {
    dependsOn("verifyNatives")
    from(rootProject.file("LICENSE"))
}
