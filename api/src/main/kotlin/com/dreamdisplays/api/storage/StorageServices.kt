package com.dreamdisplays.api.storage

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.display.model.ClientSettingsStorage
import com.dreamdisplays.api.runtime.registry.ServiceKey
import com.dreamdisplays.api.runtime.registry.serviceKey

/**
 * Storage service keys.
 *
 * @since 1.8.4
 */
@DreamDisplaysUnstableApi
object StorageServices {
    /** Server-authoritative display snapshot registry. */
    val DISPLAY_STORAGE: ServiceKey<DisplayStorage> = serviceKey("dreamdisplays:display_storage")

    /** Client-local per-display settings store. */
    val CLIENT_SETTINGS: ServiceKey<ClientSettingsStorage> = serviceKey("dreamdisplays:client_settings_storage")
}
