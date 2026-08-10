package com.dreamdisplays.core.runtime

import com.dreamdisplays.api.runtime.module.ModuleContext
import com.dreamdisplays.api.runtime.registry.ServiceRegistry

/** Default [ModuleContext] backed by a [ServiceRegistry]. */
class DefaultModuleContext(
    override val services: ServiceRegistry,
) : ModuleContext
