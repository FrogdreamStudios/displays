package com.dreamdisplays.api.media.player

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/**
 * Purges any cached resolution for a media URL so the next resolve hits the network fresh.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
fun interface CacheInvalidator {
    fun invalidate(url: String)
}
