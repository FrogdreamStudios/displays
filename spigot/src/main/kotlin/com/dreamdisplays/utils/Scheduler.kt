package com.dreamdisplays.utils

import com.dreamdisplays.Main
import org.bukkit.scheduler.BukkitRunnable
import org.jspecify.annotations.NullMarked
import java.lang.reflect.Proxy

@NullMarked
object Scheduler {

    // Runs a task asynchronously, compatible with both Bukkit and Folia
    fun runAsync(task: Runnable) {
        if (Main.getIsFolia()) {
            runFoliaAsync(task)
        } else {
            object : BukkitRunnable() {
                override fun run() = task.run()
            }.runTaskAsynchronously(Main.getInstance())
        }
    }

    // Runs a task asynchronously on Folia's async scheduler
    private fun runFoliaAsync(task: Runnable) {
        try {
            val bukkitClass = Class.forName("org.bukkit.Bukkit")
            val asyncScheduler = bukkitClass.getMethod("getAsyncScheduler").invoke(null)
            val consumerClass = Class.forName("java.util.function.Consumer")

            val consumerTask = Proxy.newProxyInstance(
                consumerClass.classLoader,
                arrayOf(consumerClass)
            ) { _, _, _ ->
                task.run()
                null
            }

            asyncScheduler.javaClass
                .getMethod("runNow", Any::class.java, consumerClass)
                .invoke(asyncScheduler, Main.getInstance(), consumerTask)
        } catch (_: Exception) {
            // Fallback to direct execution if reflection fails
            task.run()
        }
    }

    // Runs a task synchronously
    // Compatible with both Bukkit and Folia
    fun runSync(task: Runnable) {
        if (Main.getIsFolia()) {
            runFoliaSync(task)
        } else {
            object : BukkitRunnable() {
                override fun run() = task.run()
            }.runTask(Main.getInstance())
        }
    }

    // Runs a task synchronously on Folia's global region scheduler
    private fun runFoliaSync(task: Runnable) {
        try {
            val bukkitClass = Class.forName("org.bukkit.Bukkit")
            val globalRegionScheduler = bukkitClass.getMethod("getGlobalRegionScheduler").invoke(null)
            val consumerClass = Class.forName("java.util.function.Consumer")

            val consumerTask = Proxy.newProxyInstance(
                consumerClass.classLoader,
                arrayOf(consumerClass)
            ) { _, _, _ ->
                task.run()
                null
            }

            globalRegionScheduler.javaClass
                .getMethod("run", Any::class.java, consumerClass)
                .invoke(globalRegionScheduler, Main.getInstance(), consumerTask)
        } catch (_: Exception) {
            // Fallback to direct execution if reflection fails
            task.run()
        }
    }
}
