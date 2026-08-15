package com.dreamdisplays.api.render.texture.service

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/**
 * Supplies the [TextureUploaderFactory].
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
fun interface TextureUploaderProvider {
    /** Creates the factory instance. */
    fun create(): TextureUploaderFactory
}
