package com.dreamdisplays.utils

import com.dreamdisplays.Main.Companion.modVersion
import com.dreamdisplays.Main.Companion.pluginLatestVersion
import com.github.zafarkhaja.semver.Version
import me.inotsleep.utils.logging.LoggingManager.warn
import java.util.regex.Pattern

/**
 * Checks for updates of the plugin and mod from GitHub releases.
 */
object Updater {
    private val tailPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?")

    fun checkForUpdates(repoOwner: String, repoName: String) {
        try {
            val releases = GitHubFetcher.fetchReleases(repoOwner, repoName)
            if (releases.isEmpty()) return

            modVersion = releases
                .mapNotNull { parseVersion(it.tagName) }
                .filter { !it.toString().contains("-SNAPSHOT") }
                .maxOrNull()

            pluginLatestVersion = releases
                .filter {
                    it.tagName.contains("spigot", ignoreCase = true) ||
                            it.tagName.contains("plugin", ignoreCase = true)
                }
                .mapNotNull { parseVersion(it.tagName)?.toString() }
                .filter { !it.contains("-SNAPSHOT") }
                .maxOrNull() ?: modVersion?.toString()

        } catch (e: Exception) {
            warn("Unable to load versions from GitHub", e)
        }
    }

    private fun parseVersion(tag: String): Version? {
        val matcher = tailPattern.matcher(tag)
        return if (matcher.find()) runCatching { Version.parse(matcher.group()) }.getOrNull()
        else null
    }
}
