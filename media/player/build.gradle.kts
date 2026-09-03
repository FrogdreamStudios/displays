plugins {
    id("dreamdisplays.kotlin-conventions")
}
dependencies {
    api(project(":api"))
    api(project(":media:runtime"))
    api(project(":util"))
    api(libs.commonsCompress)
    api(libs.tukaaniXz)
    api(libs.javacv)
    api(libs.ffmpegPlatform)
    implementation("net.java.dev.jna:jna:5.13.0@jar")
    compileOnly("org.lwjgl:lwjgl:3.3.6")
    compileOnly("org.lwjgl:lwjgl-openal:3.3.6")
    compileOnly(libs.slf4jApi)
    testImplementation(libs.slf4jApi)
}