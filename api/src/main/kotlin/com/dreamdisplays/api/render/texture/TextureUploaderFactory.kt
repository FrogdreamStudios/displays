package com.dreamdisplays.api.render.texture

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/** Creates [TextureUploader] instances per GL context (popout window, PiP overlay, etc.). */
@DreamDisplaysUnstableApi
fun interface TextureUploaderFactory {
    /** @param stateCache true to route GL calls through Minecraft's cached `GlStateManager`. */
    fun create(stateCache: Boolean): TextureUploader
}
