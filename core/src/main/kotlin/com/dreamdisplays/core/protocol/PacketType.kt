package com.dreamdisplays.core.protocol

import com.dreamdisplays.api.protocol.PacketDirection
import com.dreamdisplays.core.protocol.packets.ClearCache
import com.dreamdisplays.core.protocol.packets.ClientHello
import com.dreamdisplays.core.protocol.packets.DreamPacket
import com.dreamdisplays.core.protocol.packets.DisplayDelete
import com.dreamdisplays.core.protocol.packets.DisplayInfo
import com.dreamdisplays.core.protocol.packets.DisplaySync
import com.dreamdisplays.core.protocol.packets.FullscreenAck
import com.dreamdisplays.core.protocol.packets.FullscreenState
import com.dreamdisplays.core.protocol.packets.PipPin
import com.dreamdisplays.core.protocol.packets.PlaybackCommand
import com.dreamdisplays.core.protocol.packets.RadiusPreview
import com.dreamdisplays.core.protocol.packets.RemotePlaybackToggle
import com.dreamdisplays.core.protocol.packets.ReportDisplay
import com.dreamdisplays.core.protocol.packets.ReportDuration
import com.dreamdisplays.core.protocol.packets.RequestSync
import com.dreamdisplays.core.protocol.packets.ServerHello
import com.dreamdisplays.core.protocol.packets.SetDisplaysEnabled
import com.dreamdisplays.core.protocol.packets.SetLocked
import com.dreamdisplays.core.protocol.packets.SetMode
import com.dreamdisplays.core.protocol.packets.SetVideo
import com.dreamdisplays.core.protocol.packets.WatchPartyControl
import com.dreamdisplays.core.protocol.packets.WatchPartyStart
import com.dreamdisplays.core.protocol.packets.WatchPartyState
import kotlin.reflect.KClass

/**
 * Append-only protocol-v2 packet type ids; wire-protocol stable, never reuse or renumber.
 */
enum class PacketType(
    val id: Int,
    val packetClass: KClass<out DreamPacket>,
    val direction: PacketDirection,
) {
    CLIENT_HELLO(1, ClientHello::class, PacketDirection.CLIENT_TO_SERVER),
    SERVER_HELLO(2, ServerHello::class, PacketDirection.SERVER_TO_CLIENT),
    DISPLAY_INFO(3, DisplayInfo::class, PacketDirection.SERVER_TO_CLIENT),
    DISPLAY_DELETE(4, DisplayDelete::class, PacketDirection.BIDIRECTIONAL),
    DISPLAY_SYNC(5, DisplaySync::class, PacketDirection.BIDIRECTIONAL),
    REQUEST_SYNC(6, RequestSync::class, PacketDirection.CLIENT_TO_SERVER),
    SET_VIDEO(7, SetVideo::class, PacketDirection.CLIENT_TO_SERVER),
    SET_LOCKED(8, SetLocked::class, PacketDirection.CLIENT_TO_SERVER),
    REPORT_DISPLAY(9, ReportDisplay::class, PacketDirection.CLIENT_TO_SERVER),
    SET_DISPLAYS_ENABLED(10, SetDisplaysEnabled::class, PacketDirection.BIDIRECTIONAL),
    CLEAR_CACHE(11, ClearCache::class, PacketDirection.SERVER_TO_CLIENT),
    PLAYBACK_COMMAND(12, PlaybackCommand::class, PacketDirection.CLIENT_TO_SERVER),
    SET_MODE(13, SetMode::class, PacketDirection.CLIENT_TO_SERVER),
    WATCH_PARTY_START(14, WatchPartyStart::class, PacketDirection.CLIENT_TO_SERVER),
    WATCH_PARTY_CONTROL(15, WatchPartyControl::class, PacketDirection.CLIENT_TO_SERVER),
    WATCH_PARTY_STATE(16, WatchPartyState::class, PacketDirection.SERVER_TO_CLIENT),
    FULLSCREEN_STATE(17, FullscreenState::class, PacketDirection.SERVER_TO_CLIENT),
    FULLSCREEN_ACK(18, FullscreenAck::class, PacketDirection.CLIENT_TO_SERVER),
    RADIUS_PREVIEW(19, RadiusPreview::class, PacketDirection.SERVER_TO_CLIENT),
    PIP_PIN(20, PipPin::class, PacketDirection.CLIENT_TO_SERVER),
    REPORT_DURATION(21, ReportDuration::class, PacketDirection.CLIENT_TO_SERVER),
    REMOTE_PLAYBACK_TOGGLE(22, RemotePlaybackToggle::class, PacketDirection.SERVER_TO_CLIENT);

    companion object {
        private val byId = entries.associateBy { it.id }

        init {
            require(byId.size == entries.size) { "Duplicate protocol packet type ids." }
        }

        fun fromId(id: Int): PacketType? = byId[id]
    }
}
