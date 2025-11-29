package com.dreamdisplays.screen

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.inotsleep.utils.logging.LoggingManager
import org.jspecify.annotations.NullMarked
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages client display settings.
 */
@NullMarked
object Settings {
    // TODO: move to adequate path
    private val SETTINGS_FILE = File("./config/dreamdisplays/client-display-settings.json")
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val displaySettings: MutableMap<UUID, DisplaySettings> = HashMap<UUID, DisplaySettings>(64)
    private val saveExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "DreamDisplays-SettingsSaver").apply { isDaemon = true }
    }
    private val pendingSave = AtomicBoolean(false)
    private const val SAVE_DELAY_MS = 500L

    // Load settings from disk
    fun load() {
        val dir = SETTINGS_FILE.getParentFile()
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            LoggingManager.error("Failed to create settings directory.")
            return
        }

        if (!SETTINGS_FILE.exists()) {
            return
        }

        try {
            BufferedReader(FileReader(SETTINGS_FILE), 8192).use { reader ->
                val type = object : TypeToken<MutableMap<String, DisplaySettings>>() {}.type
                val loadedSettings = GSON.fromJson<MutableMap<String, DisplaySettings>>(reader, type)
                if (loadedSettings != null) {
                    displaySettings.clear()
                    if (loadedSettings.isNotEmpty()) {
                        for (entry in loadedSettings.entries) {
                            try {
                                val uuid = UUID.fromString(entry.key)
                                displaySettings[uuid] = entry.value
                            } catch (_: IllegalArgumentException) {
                                LoggingManager.error("Invalid UUID in client display settings: ${entry.key}")
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            LoggingManager.error("Failed to load client display settings", e)
        }
    }

    // Save settings to disk
    private fun saveInternal() {
        try {
            val dir = SETTINGS_FILE.getParentFile()
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                LoggingManager.error("Failed to create settings directory.")
                return
            }

            val toSave: MutableMap<String, DisplaySettings> = HashMap<String, DisplaySettings>(displaySettings.size)
            for (entry in displaySettings.entries) {
                toSave[entry.key.toString()] = entry.value
            }

            BufferedWriter(FileWriter(SETTINGS_FILE), 8192).use { writer ->
                GSON.toJson(toSave, writer)
            }
        } catch (e: IOException) {
            LoggingManager.error("Failed to save client display settings", e)
        } finally {
            pendingSave.set(false)
        }
    }

    // Save settings to disk asynchronously with debouncing
    fun save() {
        if (pendingSave.compareAndSet(false, true)) {
            saveExecutor.schedule({
                saveInternal()
            }, SAVE_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    // Get settings for a display
    @JvmStatic
    fun getSettings(displayId: UUID): DisplaySettings {
        return displaySettings.computeIfAbsent(displayId) { _: UUID? -> DisplaySettings() }
    }

    // Update settings for a display
    @JvmStatic
    fun updateSettings(displayId: UUID, volume: Float, quality: String, muted: Boolean) {
        val settings = getSettings(displayId)
        settings.volume = volume
        settings.quality = quality
        settings.muted = muted

        save()
    }

    class DisplaySettings {
        @JvmField
        var volume: Float = 0.5f
        @JvmField
        var quality: String = "720"
        @JvmField
        var muted: Boolean = false

        constructor()

    }
}
