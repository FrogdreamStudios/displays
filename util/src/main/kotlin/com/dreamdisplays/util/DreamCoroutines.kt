package com.dreamdisplays.util

import kotlinx.coroutines.*

/** Shared client-side coroutine scope for background work, replaces per-subsystem `java.util.concurrent`. */
object DreamCoroutines {
    /** Client-side coroutine scope for blocking IO. */
    val clientIo: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("DD-IO"))

    /** Cancels all background client coroutines. Called on client shutdown. */
    fun shutdown() {
        clientIo.cancel()
    }
}
