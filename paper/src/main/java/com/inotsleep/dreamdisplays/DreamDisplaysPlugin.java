package com.inotsleep.dreamdisplays;

import com.github.zafarkhaja.semver.Version;
import com.inotsleep.dreamdisplays.commands.DisplayCommand;
import com.inotsleep.dreamdisplays.listeners.PlayerListener;
import com.inotsleep.dreamdisplays.listeners.SelectionListener;
import com.inotsleep.dreamdisplays.managers.DisplayManager;
import com.inotsleep.dreamdisplays.storage.Storage;
import com.inotsleep.dreamdisplays.utils.GithubReleaseFetcher;
import com.inotsleep.dreamdisplays.utils.net.PacketReceiver;
import me.inotsleep.utils.AbstractPlugin;
import me.inotsleep.utils.logging.LoggingManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DreamDisplaysPlugin extends AbstractPlugin<DreamDisplaysPlugin> {

    public static Config config;
    public Storage storage;

    public static Version modVersion = null;

    @Override
    public void doEnable() {
        config = new Config(this);
        config.reload();

        storage = new Storage(this);

        registerChannels();
        registerCommands();

        Bukkit.getPluginManager().registerEvents(new SelectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);

        new BukkitRunnable() {
            public void run() {
                DisplayManager.updateAllDisplays();
            }
        }.runTaskTimerAsynchronously(this, 0, 20);

        new BukkitRunnable() {
            public void run() {
                try {
                    List<GithubReleaseFetcher.Release> realises = GithubReleaseFetcher.fetchReleases(config.settings.repoOwner, config.settings.repoName);
                    LoggingManager.info("Found " + realises.size() + " Github releases");
                    if (realises.isEmpty()) return;

                    modVersion = realises.stream().map((r) -> Version.parse(extractTail(r.tagName()))).max(Comparator.naturalOrder()).get();
                    LoggingManager.info("Latest mod version: " + modVersion);
                } catch (Exception e) {
                    LoggingManager.warn("Unable to load mod version", e);
                }
            }
        }.runTaskTimerAsynchronously(this, 0, 20 * 3600);
    }

    private static final Pattern TAIL_PATTERN = Pattern.compile("\\d[\\s\\S]*");

    private static String extractTail(String input) {
        Matcher m = TAIL_PATTERN.matcher(input);
        return m.find() ? m.group() : "";
    }

    @Override
    public void doDisable() {
        storage.onDisable();
    }

    public static DreamDisplaysPlugin getInstance() {
        return getInstanceByClazz(DreamDisplaysPlugin.class);
    }

    public void registerChannels() {
        Messenger messenger = getServer().getMessenger();

        // DreamDisplays channels
        messenger.registerOutgoingPluginChannel(this, "dreamdisplays:display_info");
        messenger.registerOutgoingPluginChannel(this, "dreamdisplays:sync");
        messenger.registerOutgoingPluginChannel(this, "dreamdisplays:delete");
        messenger.registerOutgoingPluginChannel(this, "dreamdisplays:premium");

        // FrogDisplays channels
        messenger.registerOutgoingPluginChannel(this, "frogdisplays:display_info");
        messenger.registerOutgoingPluginChannel(this, "frogdisplays:sync");
        messenger.registerOutgoingPluginChannel(this, "frogdisplays:delete");
        messenger.registerOutgoingPluginChannel(this, "frogdisplays:premium");

        PacketReceiver receiver = new PacketReceiver(this);

        // DreamDisplays incoming channels
        messenger.registerIncomingPluginChannel(this, "dreamdisplays:sync", receiver);
        messenger.registerIncomingPluginChannel(this, "dreamdisplays:req_sync", receiver);
        messenger.registerIncomingPluginChannel(this, "dreamdisplays:delete", receiver);
        messenger.registerIncomingPluginChannel(this, "dreamdisplays:report", receiver);
        messenger.registerIncomingPluginChannel(this, "dreamdisplays:version", receiver);

        // FrogDisplays incoming channels
        messenger.registerIncomingPluginChannel(this, "frogdisplays:sync", receiver);
        messenger.registerIncomingPluginChannel(this, "frogdisplays:req_sync", receiver);
        messenger.registerIncomingPluginChannel(this, "frogdisplays:delete", receiver);
        messenger.registerIncomingPluginChannel(this, "frogdisplays:report", receiver);
        messenger.registerIncomingPluginChannel(this, "frogdisplays:version", receiver);
    }

    public void registerCommands() {
        new DisplayCommand();
    }
}