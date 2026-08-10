package com.dreamdisplays.api.platform.integration

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.platform.identity.Platform

/**
 * Supplies the [Platform] adapter.
 *
 * @since 1.8.4
 */
@DreamDisplaysUnstableApi
fun interface PlatformIntegrationProvider {
    /** Creates the platform adapter instance. */
    fun create(): Platform
}
