package com.dreamdisplays.api.media.player

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/**
 * Runs a task on the platform's render thread (e.g. `Minecraft.getInstance().execute`).
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
fun interface RenderThreadExecutor {
    fun execute(task: () -> Unit)
}
