plugins {
    id("net.neoforged.moddev") version libs.versions.moddev
}

dependencies {
    api(libs.gst1)
	api(libs.utils)
	api(libs.javatube)
    compileOnly(libs.jna)
    compileOnly(libs.jnaPlatform)
	compileOnly(libs.lwjgl)
}

neoForge {
    neoFormVersion = libs.versions.neoform.get()
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
