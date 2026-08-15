package com.dreamdisplays.api.playback.model

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/**
 * Inputs the permission rules need.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
data class PlaybackContext(
    val mode: PlaybackMode,
    val isOwner: Boolean,
    val isAdmin: Boolean,
    val isLocked: Boolean,
    val hasActiveParty: Boolean = false,
    val isPartyHost: Boolean = false,
)
