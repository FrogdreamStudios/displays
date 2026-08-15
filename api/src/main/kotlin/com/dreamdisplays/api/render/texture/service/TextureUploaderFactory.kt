package com.dreamdisplays.api.render.texture.service

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/** Creates [TextureUploaderService] instances per GL context (popout window, PiP overlay, etc.). */
@DreamDisplaysUnstableApi
fun interface TextureUploaderFactory {
    /** @param stateCache true to route GL calls through Minecraft's cached `GlStateManager`. */
    fun create(stateCache: Boolean): TextureUploaderService
}
