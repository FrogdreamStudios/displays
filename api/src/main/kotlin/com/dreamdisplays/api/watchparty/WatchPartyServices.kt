package com.dreamdisplays.api.watchparty

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.runtime.registry.ServiceKey
import com.dreamdisplays.api.runtime.registry.serviceKey

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
