plugins {
    id("net.neoforged.moddev") version "2.0.115"
}

group = "dreamdisplays"

dependencies {
	compileOnly(libs.gst1)
	compileOnly(libs.utils)
	compileOnly(libs.javatube)
    compileOnly(libs.jna)
    compileOnly(libs.jnaPlatform)
	compileOnly(libs.lwjgl)
}

neoForge {
    enable {
        version = "21.10.10-beta"
    }
}
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = Charsets.UTF_8.name()
}

tasks.jar {
	from(rootProject.file("LICENSE"))
}
