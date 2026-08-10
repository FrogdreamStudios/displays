package com.dreamdisplays.platform.client.platform

import com.dreamdisplays.api.platform.identity.Platform
import com.dreamdisplays.api.platform.integration.PlatformIntegrationProvider

/** Supplies the `NeoForge` [Platform] adapter. */
object NeoForgePlatformIntegrationProvider : PlatformIntegrationProvider {
    override fun create(): Platform = NeoForgePlatform
}
