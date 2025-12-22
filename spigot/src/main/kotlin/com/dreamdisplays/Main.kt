package com.dreamdisplays

import com.dreamdisplays.commands.DisplayCommand
import com.dreamdisplays.listeners.Player
import com.dreamdisplays.listeners.Selection
import com.dreamdisplays.managers.DisplayManager
import com.dreamdisplays.scheduler.ProviderScheduler
import com.dreamdisplays.managers.StorageManager
import com.dreamdisplays.utils.Updater
import com.github.zafarkhaja.semver.Version
import me.inotsleep.utils.AbstractPlugin
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.jspecify.annotations.NullMarked

@NullMarked
class Main : AbstractPlugin<Main>() {

    lateinit var storage: StorageManager
    var audiences: BukkitAudiences? = null

    override fun onEnable() = try {
        super.onEnable()
    } catch (e: NoSuchMethodError) {
        if (!e.message.orEmpty().contains("getMinecraftVersion")) throw e
        doEnable()
    }

    override fun doEnable() {
        try {
            audiences = BukkitAudiences.create(this)
        } catch (_: Exception) {
            logger.warning("Adventure API not supported on this server. Using legacy messaging.")
            audiences = null
        }

        Companion.config = Config(this)
        storage = StorageManager(this)

        registerChannels()
        registerCommands()

        Bukkit.getPluginManager().registerEvents(Selection(this), this)
        Bukkit.getPluginManager().registerEvents(Player(), this)

        // Initialize bStats metrics
        Metrics(this, 26488)

        // Updating displays
        ProviderScheduler.adapter.runRepeatingAsync(
            this, 50L, 1000L
        ) { DisplayManager.updateAllDisplays() }

        // GitHub update checks
        if (Companion.config.settings.updatesEnabled) {
            ProviderScheduler.adapter.runRepeatingAsync(
                this, 20L, 20L * 3600L
            ) { Updater.checkForUpdates() }
        }
    }

    override fun doDisable() {
        audiences?.close()
        storage.onDisable()
    }

    fun registerCommands() {
        val displayCommand = DisplayCommand()
        getCommand("display")?.setExecutor(displayCommand)
        getCommand("display")?.tabCompleter = displayCommand
    }

    private fun registerChannels() {
        val messenger = server.messenger
        val receiver = com.dreamdisplays.utils.net.Receiver(this)

        listOf(
            "dreamdisplays:sync",
            "dreamdisplays:req_sync",
            "dreamdisplays:delete",
            "dreamdisplays:report",
            "dreamdisplays:version",
            "dreamdisplays:display_enabled"
        ).forEach { messenger.registerIncomingPluginChannel(this, it, receiver) }

        listOf(
            "dreamdisplays:premium",
            "dreamdisplays:display_info",
            "dreamdisplays:sync",
            "dreamdisplays:delete",
            "dreamdisplays:report_enabled"
        ).forEach { messenger.registerOutgoingPluginChannel(this, it) }
    }

    companion object {
        lateinit var config: Config
        var modVersion: Version? = null
        var pluginLatestVersion: String? = null

        fun getInstance(): Main = getInstanceByClazz(Main::class.java)

        fun getIsFolia(): Boolean = runCatching {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        }.isSuccess

        fun disablePlugin() {
            getInstance().server.pluginManager.disablePlugin(getInstance())
        }
    }
}
