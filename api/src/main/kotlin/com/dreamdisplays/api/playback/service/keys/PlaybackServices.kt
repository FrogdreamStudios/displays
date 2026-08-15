package com.dreamdisplays.api.playback.service.keys

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.playback.service.PlaybackService
import com.dreamdisplays.api.runtime.registry.model.ServiceKey
import com.dreamdisplays.api.runtime.registry.model.serviceKey

/**
 * Playback service keys.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
object PlaybackServices {
    /** Public display playback command surface. */
    val PLAYBACK: ServiceKey<PlaybackService> = serviceKey("dreamdisplays:playback")
}
