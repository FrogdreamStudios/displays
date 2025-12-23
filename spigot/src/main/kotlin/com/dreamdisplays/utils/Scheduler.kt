package com.dreamdisplays.utils

import com.dreamdisplays.Main
import org.bukkit.scheduler.BukkitRunnable
import org.jspecify.annotations.NullMarked
import java.lang.reflect.Proxy

/**
 * Scheduler utility to run tasks synchronously or asynchronously,
 * supporting both standard Bukkit and Folia server implementations.
 */
@NullMarked
object Scheduler {
    private val isFolia: Boolean by lazy { Main.getIsFolia() }
    private val plugin: Main by lazy { Main.getInstance() }

    // Run async
    fun runAsync(task: Runnable) {
        if (isFolia) {
            runFoliaAsync(task)
        } else {
            BukkitTask(task).runTaskAsynchronously(plugin)
        }
    }

    // Run sync
    fun runSync(task: Runnable) {
        if (isFolia) {
            runFoliaSync(task)
        } else {
            BukkitTask(task).runTask(plugin)
        }
    }

    // Folia async
    private fun runFoliaAsync(task: Runnable) {
        runCatching {
            val asyncScheduler = Class.forName("org.bukkit.Bukkit")
                .getMethod("getAsyncScheduler")
                .invoke(null)

            val consumerTask = createConsumerProxy(task)

            asyncScheduler.javaClass
                .getMethod("runNow", Any::class.java, Class.forName("java.util.function.Consumer"))
                .invoke(asyncScheduler, plugin, consumerTask)
        }.getOrElse {
            task.run() // Fallback: run directly
        }
    }

    // Folia sync
    private fun runFoliaSync(task: Runnable) {
        runCatching {
            val globalScheduler = Class.forName("org.bukkit.Bukkit")
                .getMethod("getGlobalRegionScheduler")
                .invoke(null)

            val consumerTask = createConsumerProxy(task)

            globalScheduler.javaClass
                .getMethod("run", Any::class.java, Class.forName("java.util.function.Consumer"))
                .invoke(globalScheduler, plugin, consumerTask)
        }.getOrElse {
            task.run() // Fallback: run directly
        }
    }

    // Create Consumer proxy
    // It's needed to pass Runnable to Folia scheduler
    private fun createConsumerProxy(task: Runnable): Any {
        val consumerClass = Class.forName("java.util.function.Consumer")

        return Proxy.newProxyInstance(
            consumerClass.classLoader,
            arrayOf(consumerClass)
        ) { _, _, _ ->
            task.run()
            null
        }
    }

    // Bukkit task wrapper
    private class BukkitTask(private val task: Runnable) : BukkitRunnable() {
        override fun run() = task.run()
    }
}
