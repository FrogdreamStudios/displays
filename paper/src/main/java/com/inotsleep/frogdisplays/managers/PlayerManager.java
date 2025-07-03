package com.inotsleep.frogdisplays.managers;

import com.github.zafarkhaja.semver.Version;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerManager {
    private static final Map<UUID, Version> versions = new HashMap<>();

    public static Version getVersion(final Player player) {
        return versions.get(player.getUniqueId());
    }

    public static void setVersion(final Player player, final Version version) {
        versions.put(player.getUniqueId(), version);
    }

    public static void removeVersion(final Player player) {
        versions.remove(player.getUniqueId());
    }

    public static Collection<Version> getVersions() {
        return versions.values();
    }
}