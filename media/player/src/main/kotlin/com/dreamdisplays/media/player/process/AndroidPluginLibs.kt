package com.dreamdisplays.media.player.process

import java.io.File

/**
 * Resolves native libraries shipped inside the Pojav ffmpeg plugin APK
 * (`net.kdt.pojavlaunch.ffmpeg`) — `libffmpeg.so`, `libaaudio_sink.so`, `libytdlp.so` all live
 * side by side in that plugin's `lib/<abi>/` folder, so once ffmpeg's own path is known
 * (`POJAV_FFMPEG_PATH`, set by the launcher) the others are found by swapping the file name.
 * On Android these binaries are bundled with the plugin instead of being downloaded, unlike the
 * desktop [FFmpegBinary] flow.
 */
object AndroidPluginLibs {
    private const val ENV_FFMPEG_PATH = "POJAV_FFMPEG_PATH"

    /** Absolute path to `libffmpeg.so` from the installed ffmpeg plugin, or null off-Android. */
    fun ffmpegPath(): String? =
        System.getenv(ENV_FFMPEG_PATH)?.takeIf { File(it).isFile }

    /** Absolute path to `libaaudio_sink.so`, resolved from ffmpeg's own plugin lib directory. */
    fun aaudioSinkPath(): String? = siblingOf("libaaudio_sink.so")

    /** Absolute path to `libytdlp.so`, resolved from ffmpeg's own plugin lib directory. */
    fun ytdlpPath(): String? = siblingOf("libytdlp.so")

    private fun siblingOf(fileName: String): String? {
        val ffmpeg = ffmpegPath()?.let(::File) ?: return null
        val sibling = File(ffmpeg.parentFile, fileName)
        return sibling.takeIf { it.isFile }?.absolutePath
    }
}
