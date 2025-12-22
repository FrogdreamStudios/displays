package com.dreamdisplays.scheduler

import com.dreamdisplays.Main
import org.jspecify.annotations.NullMarked

@NullMarked
object ProviderScheduler {
    val adapter: AdapterScheduler =
        if (Main.getIsFolia()) FoliaScheduler else BukkitScheduler
}
