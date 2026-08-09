package com.dreamdisplays.api.runtime

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/** `Dream Displays` module; registers services, providers, or listeners through [ModuleContext]. */
@DreamDisplaysUnstableApi
interface DreamDisplaysModule {
    /** Unique module id, preferably in `namespace:name` form. */
    val id: String

    /** Module ids that must be installed before this module. */
    val dependencies: List<String> get() = emptyList()

    /** Installs this module into [context]. Called once after all dependencies are installed. */
    fun install(context: ModuleContext)

    /** Removes runtime hooks registered by this module. Called in reverse install order. */
    fun uninstall(context: ModuleContext) {}
}
