package com.dreamdisplays.platform.client.platform

import com.dreamdisplays.api.platform.identity.Platform
import com.dreamdisplays.api.platform.integration.PlatformIntegrationProvider

/** Supplies the `Fabric` [Platform] adapter. */
object FabricPlatformIntegrationProvider : PlatformIntegrationProvider {
    override fun create(): Platform = FabricPlatform
}
