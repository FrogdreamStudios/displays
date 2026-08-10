package com.dreamdisplays.api.runtime.module

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.runtime.registry.ServiceRegistry

/**
 * Entry point for services exposed to integrations and modules.
 *
 * @since 1.8.4
 */
@DreamDisplaysUnstableApi
interface DreamDisplaysApi {
    /** Contract-typed services available in the current runtime. */
    val services: ServiceRegistry
}
