import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("fabric-loom") version libs.versions.loom
	id("maven-publish")
	id("com.gradleup.shadow") version libs.versions.shadow
}

group = "dreamdisplays"

dependencies {
	minecraft(libs.fabricMinecraft)
	mappings(loom.officialMojangMappings())
	modImplementation(libs.fabricLoader)
	modImplementation(libs.fabricApi)
	shadow(libs.gst1)
	shadow(libs.utils)
	shadow(libs.javatube)
    compileOnly(libs.jna)
    compileOnly(libs.jnaPlatform)
	compileOnly(libs.lwjgl)
}

tasks.processResources {
	val projectVersion = project.version.toString()
	inputs.property("version", projectVersion)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to projectVersion))
    }
    filesMatching("version") {
        expand(mapOf("version" to projectVersion))
    }
}

java {
	withSourcesJar()
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = Charsets.UTF_8.name()
}

tasks.jar {
	from(rootProject.file("LICENSE"))
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("shadow"))
    dependencies {
        include(dependency("org.freedesktop.gstreamer:gst1-java-core"))
        include(dependency("com.github.felipeucelli:javatube"))
        include(dependency("org.json:json"))
        include(dependency("me.inotsleep:utils"))
    }
}
tasks.withType<RemapJarTask>().configureEach {
	inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })

	archiveClassifier = ""
	destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))

	archiveBaseName = "dreamdisplays-fabric"
	archiveVersion.set(rootProject.version.toString())
}
