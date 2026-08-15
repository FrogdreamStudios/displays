package com.dreamdisplays.api.storage.service

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.display.model.settings.ClientSettingsStorage

/**
 * Supplies the storage backends.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
interface StorageProvider {
    /** The server-authoritative display snapshot registry. */
    fun displayStorage(): DisplayStorageService

    /** The client-local per-display settings store. */
    fun clientSettingsStorage(): ClientSettingsStorage
}
