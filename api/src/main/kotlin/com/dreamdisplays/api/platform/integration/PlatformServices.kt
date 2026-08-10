package com.dreamdisplays.api.platform.integration

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.platform.identity.Platform
import com.dreamdisplays.api.runtime.registry.ServiceKey
import com.dreamdisplays.api.runtime.registry.serviceKey

/**
 * Platform service keys.
 *
 * @since 1.8.4
 */
@DreamDisplaysUnstableApi
object PlatformServices {
    /** Platform service. */
    val PLATFORM: ServiceKey<Platform> = serviceKey("dreamdisplays:platform")
}
