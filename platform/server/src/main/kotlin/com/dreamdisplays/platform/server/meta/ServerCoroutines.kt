package com.dreamdisplays.platform.server.meta

import kotlinx.coroutines.*

/**
 * Server-side coroutine scope for pure off-thread IO that never touches `Bukkit` / `Minecraft` state — webhooks, HTTP calls,
 * and similar.
 */
object ServerCoroutines {
    /** The server-side coroutine scope for all background IO. */
    val io: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("Server-IO"))

    /** Cancels all background server IO coroutines. Called on server stop / plugin disable. */
    fun shutdown() {
        io.cancel()
    }
}
