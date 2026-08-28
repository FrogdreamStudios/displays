plugins {
    id("dreamdisplays.kotlin-conventions")
    id("dreamdisplays.serialization-conventions")
    id("dreamdisplays.shadow-conventions")
    alias(libs.plugins.platformweaver)
}

val generatedVersionDir = layout.buildDirectory.dir("generated/source/pluginVersion/kotlin")

val generatePluginVersion by tasks.registering {
    description = "Generates version of proxy plugin."
    val outputDir = generatedVersionDir
    val versionValue = rootProject.version.toString()
    inputs.property("version", versionValue)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/dreamdisplays/platform/proxy/velocity/PluginVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.dreamdisplays.platform.proxy.velocity

            internal const val PLUGIN_VERSION = "$versionValue"

            """.trimIndent() + "\n",
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generatedVersionDir)
}

tasks.compileKotlin {
    dependsOn(generatePluginVersion)
}

platformweaver {
    target = "velocity"
    chameleonsDir = null
}

dependencies {
    compileOnly(libs.platformweaverAnnotations)
    compileOnly(libs.velocityApi)
    compileOnly(libs.bungeecordApi)
    compileOnly(libs.slf4jApi)

    implementation(project(":platform:proxy:common"))
    implementation(project(":core"))
    implementation(libs.kotlinStdlib)
    implementation(libs.kotlinxSerializationProtobuf)
}

tasks.processResources {
    val props = mapOf("version" to rootProject.version.toString())
    inputs.properties(props)
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("velocity-plugin.json") {
        expand(props)
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("dreamdisplays-velocity")
    val prefix = "com.dreamdisplays.libs"
    listOf(
        "kotlin",
        "kotlinx.serialization",
        "kotlinx.io",
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    archiveVersion.set(rootProject.version.toString())
}
