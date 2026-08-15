package com.dreamdisplays.core.services

import com.dreamdisplays.api.display.event.DisplayEvent
import com.dreamdisplays.api.display.model.Display
import com.dreamdisplays.api.display.model.property.DisplayId
import com.dreamdisplays.api.display.model.property.DisplayState
import com.dreamdisplays.api.display.model.settings.DisplaySettings
import com.dreamdisplays.api.display.service.DisplayLookup
import com.dreamdisplays.api.display.service.DisplayMutationPort
import com.dreamdisplays.api.display.service.DisplayService
import com.dreamdisplays.api.media.model.VideoQuality
import com.dreamdisplays.api.playback.model.PlaybackMode
import com.dreamdisplays.api.playback.service.PlaybackPort
import com.dreamdisplays.api.playback.service.PlaybackService
import com.dreamdisplays.api.watchparty.service.WatchPartyPort
import com.dreamdisplays.api.watchparty.service.WatchPartyService
import com.dreamdisplays.api.watchparty.model.WatchPartySession
import kotlin.time.Duration

/**
 * Default core implementation of [DisplayService].
 */
class DefaultDisplayService(
    private val lookup: DisplayLookup,
    private val mutations: DisplayMutationPort,
) : DisplayService {
    /** Get the display with the given [id], or null if it doesn't exist. */
    override fun getDisplay(id: DisplayId): Display? = lookup.getDisplay(id)

    /** Returns all displays currently visible to this service. */
    override fun listDisplays(): List<Display> = lookup.listDisplays()

    /** Replaces client-local or server-authoritative settings for [id], depending on implementation side. */
    override fun updateSettings(id: DisplayId, settings: DisplaySettings) = mutations.updateSettings(id, settings)

    /** Requests a server-authoritative video change for [id], optionally with the audio-track [lang]. */
    override fun setUrl(id: DisplayId, url: String?, lang: String?) = mutations.setUrl(id, url, lang)

    /** Locks or unlocks [id] (owner / admin); the server validates and echoes the new state. */
    override fun setLocked(id: DisplayId, locked: Boolean) = mutations.setLocked(id, locked)

    /** Deletes [id] entirely: purges its persisted data and unregisters it (owner / admin). */
    override fun delete(id: DisplayId) = mutations.delete(id)

    /** Reports [id] for moderation review. */
    override fun report(id: DisplayId) = mutations.report(id)

    /** Subscribes [listener] to display lifecycle events; close the returned handle to unsubscribe. */
    override fun on(listener: (DisplayEvent) -> Unit): AutoCloseable = lookup.onDisplayEvent(listener)
}

/**
 * Default core implementation of [PlaybackService].
 */
class DefaultPlaybackService(
    private val playback: PlaybackPort,
) : PlaybackService {
    /** Get the playback state for the display with the given [id], or null if it doesn't exist. */
    override fun play(displayId: DisplayId) = playback.play(displayId)

    /** Pause the playback for the display with the given [id], or null if it doesn't exist. */
    override fun pause(displayId: DisplayId) = playback.pause(displayId)

    /** Stop the playback for the display with the given [id], or null if it doesn't exist. */
    override fun stop(displayId: DisplayId) = playback.stop(displayId)

    /** Seek to a specific position in the playback for the display with the given [id], or null if it doesn't exist. */
    override fun seek(displayId: DisplayId, position: Duration) = playback.seek(displayId, position)

    /** Seek [delta] relative to the current position for [displayId] (negative = backward). */
    override fun seekRelative(displayId: DisplayId, delta: Duration) = playback.seekRelative(displayId, delta)

    /** Set the volume for a display. */
    override fun setVolume(displayId: DisplayId, volume: Float) = playback.setVolume(displayId, volume)

    /** Set the preferred video quality for a display. */
    override fun setQuality(displayId: DisplayId, quality: VideoQuality) = playback.setQuality(displayId, quality)

    /** Set the active audio track for a display. */
    override fun setAudioTrack(displayId: DisplayId, trackUrl: String) = playback.setAudioTrack(displayId, trackUrl)

    /** Set the brightness multiplier for a display. */
    override fun setBrightness(displayId: DisplayId, brightness: Float) = playback.setBrightness(displayId, brightness)

    /** Mute or unmute the audio for a display. */
    override fun mute(displayId: DisplayId, muted: Boolean) = playback.mute(displayId, muted)

    /** Get the runtime state for a display. */
    override fun getState(displayId: DisplayId): DisplayState = playback.getState(displayId)

    /** Restart the playback for a display. */
    override fun restart(displayId: DisplayId) = playback.restart(displayId)

    /** The effective [PlaybackMode] of a display (`WATCH_PARTY` while a session is live). */
    override fun getMode(displayId: DisplayId): PlaybackMode = playback.getMode(displayId)

    /** Requests a new persistent base mode (`LOCAL` / `SYNCED` / `BROADCAST`); the server validates it. */
    override fun setMode(displayId: DisplayId, mode: PlaybackMode) = playback.setMode(displayId, mode)

    /** Re-resolves and reloads the current video for [displayId] after a load failure (local recovery). */
    override fun retry(displayId: DisplayId) = playback.retry(displayId)
}

/**
 * Default core implementation of [WatchPartyService].
 */
class DefaultWatchPartyService(
    private val watchParty: WatchPartyPort,
) : WatchPartyService {
    /** Starts a watch party session for the display with the given [id], or null if it doesn't exist. */
    override fun start(displayId: DisplayId, url: String?): Boolean = watchParty.start(displayId, url)

    /** Sets the ready state for a watch party session. */
    override fun setReady(displayId: DisplayId, ready: Boolean) = watchParty.setReady(displayId, ready)

    /** Host: starts the synchronized countdown. */
    override fun begin(displayId: DisplayId) = watchParty.begin(displayId)

    /** Host: ends the session (freezes on the final frame). */
    override fun end(displayId: DisplayId) = watchParty.end(displayId)

    /** Host / owner / admin: closes the session, returning the display to its base mode. */
    override fun restart(displayId: DisplayId) = watchParty.restartSession(displayId)

    /** Host / owner / admin: closes the session, returning the display to its base mode. */
    override fun close(displayId: DisplayId) = watchParty.close(displayId)

    /** The live session on [displayId], or null when none is running. */
    override fun getSession(displayId: DisplayId): WatchPartySession? = watchParty.getSession(displayId)
}
