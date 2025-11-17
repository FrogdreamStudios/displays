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

import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DreamDisplaysPlugin extends AbstractPlugin<DreamDisplaysPlugin> {

    public static Config config;
    public Storage storage;

    public static Version modVersion = null;

    // Check if the server is running Folia
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void doEnable() {
        config = new Config(this);
        config.reload();

        storage = new Storage(this);

        registerChannels();
        registerCommands();

        Bukkit.getPluginManager().registerEvents(new SelectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);

        Runnable updateTask = () -> DisplayManager.updateAllDisplays();

        if (isFolia()) {
            try {
                Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                Object asyncScheduler = bukkitClass.getMethod("getAsyncScheduler").invoke(null);
                Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Class<?> timeUnitClass = Class.forName("java.util.concurrent.TimeUnit");
                Object ticksUnit = timeUnitClass.getDeclaredField("MILLISECONDS").get(null);

                Object taskConsumer = Proxy.newProxyInstance(
                    consumerClass.getClassLoader(),
                    new Class<?>[]{consumerClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("accept")) {
                            updateTask.run();
                        }
                        return null;
                    }
                );

                // runAtFixedRate(Plugin, Consumer<ScheduledTask>, long, long, TimeUnit)
                asyncScheduler.getClass()
                    .getMethod("runAtFixedRate", pluginClass, consumerClass, long.class, long.class, timeUnitClass)
                    .invoke(asyncScheduler, this, taskConsumer, 50L, 1000L, ticksUnit);
            } catch (Exception e) {
                LoggingManager.warn("Failed to schedule update task on Folia: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            new BukkitRunnable() {
                public void run() {
                    updateTask.run();
                }
            }.runTaskTimerAsynchronously(this, 0, 20);
        }

        Runnable githubTask = () -> {
            try {
                List<GithubReleaseFetcher.Release> realises = GithubReleaseFetcher.fetchReleases(config.settings.repoOwner, config.settings.repoName);
                LoggingManager.info("Found " + realises.size() + " Github releases");
                if (realises.isEmpty()) return;

                modVersion = realises.stream()
                    .map(r -> extractTail(r.tagName()))
                    .filter(tag -> tag != null && !tag.trim().isEmpty())
                    .map(Version::parse)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

                if (modVersion != null) {
                    LoggingManager.info("Latest mod version: " + modVersion);
                } else {
                    LoggingManager.warn("No valid mod version found in GitHub releases");
                }
            } catch (Exception e) {
                LoggingManager.warn("Unable to load mod version", e);
            }
        };

        if (isFolia()) {
            try {
                Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                Object asyncScheduler = bukkitClass.getMethod("getAsyncScheduler").invoke(null);
                Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Class<?> timeUnitClass = Class.forName("java.util.concurrent.TimeUnit");
                Object secondsUnit = timeUnitClass.getDeclaredField("SECONDS").get(null);

                Object taskConsumer = Proxy.newProxyInstance(
                    consumerClass.getClassLoader(),
                    new Class<?>[]{consumerClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("accept")) {
                            githubTask.run();
                        }
                        return null;
                    }
                );

                // runAtFixedRate(Plugin, Consumer<ScheduledTask>, long, long, TimeUnit)
                asyncScheduler.getClass()
                    .getMethod("runAtFixedRate", pluginClass, consumerClass, long.class, long.class, timeUnitClass)
                    .invoke(asyncScheduler, this, taskConsumer, 1L, 3600L, secondsUnit);
            } catch (Exception e) {
                LoggingManager.warn("Failed to schedule github check task on Folia: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            new BukkitRunnable() {
                public void run() {
                    githubTask.run();
                }
            }.runTaskTimerAsynchronously(this, 0, 20 * 3600);
        }
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
