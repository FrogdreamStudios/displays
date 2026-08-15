package com.dreamdisplays.api.render.backend.service

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.render.service.DisplayRenderer

/**
 * Supplies the [DisplayRenderer] runtime renders registered surfaces with, so module
 * installers depend on this contract instead of the concrete renderer implementation in the platform module.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
fun interface RendererProvider {
    /** Creates the renderer instance. */
    fun create(): DisplayRenderer
}
