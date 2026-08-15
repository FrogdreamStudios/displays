package com.dreamdisplays.api.platform.service

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.platform.identity.Platform

/**
 * Supplies the [Platform] adapter.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
fun interface PlatformIntegrationService {
    /** Creates the platform adapter instance. */
    fun create(): Platform
}
