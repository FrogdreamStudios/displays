package com.dreamdisplays.core.services

import com.dreamdisplays.api.storage.model.FullDisplayData
import java.util.*
import com.dreamdisplays.api.storage.service.DisplayStorageService as DisplayStorageContract

/**
 * In-memory registry for server-authoritative display snapshots.
 */
object DisplayStorage : DisplayStorageContract {
    /** Server display snapshots, keyed by server ID and display UUID. */
    private val serverDisplays = HashMap<String, MutableMap<UUID, FullDisplayData>>()

    /** Currently active server ID, or null if none. */
    private var currentServerId: String? = null

    /** Load the display snapshot for [serverId], replacing any existing data. */
    override fun load(serverId: String, displays: Map<UUID, FullDisplayData>) {
        currentServerId = serverId
        serverDisplays[serverId] = displays.toMutableMap()
    }

    /** Clear all display snapshots for [serverId]. */
    override fun snapshot(serverId: String): Map<UUID, FullDisplayData> =
        serverDisplays[serverId]?.toMap() ?: emptyMap()

    /** Clear all display snapshots for [currentServerId]. */
    override fun currentServerId(): String? = currentServerId

    /** Get the display snapshot for [displayUuid] on the current server, or null if not found. */
    override fun getDisplayData(displayUuid: UUID): FullDisplayData? {
        val serverId = currentServerId ?: return null
        return serverDisplays[serverId]?.get(displayUuid)
    }

    /** Save the display snapshot for [displayUuid] on the current server. */
    override fun saveDisplayData(displayUuid: UUID, data: FullDisplayData) {
        val serverId = currentServerId ?: return
        serverDisplays.getOrPut(serverId) { HashMap() }[displayUuid] = data
    }

    /** Remove the display snapshot for [displayUuid] from all servers. Returns true if any were removed. */
    override fun removeDisplay(displayUuid: UUID): Boolean {
        var removed = false
        for (displays in serverDisplays.values) {
            removed = displays.remove(displayUuid) != null || removed
        }
        return removed
    }
}
