package com.dreamdisplays.api.watchparty.service.keys

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.runtime.registry.model.ServiceKey
import com.dreamdisplays.api.runtime.registry.model.serviceKey
import com.dreamdisplays.api.watchparty.service.WatchPartyService

/**
 * Watch party service keys.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
object WatchPartyServices {
    /** Public Watch party session command surface. */
    val WATCH_PARTY: ServiceKey<WatchPartyService> = serviceKey("dreamdisplays:watch_party")
}
