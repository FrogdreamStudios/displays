@file:OptIn(ExperimentalSerializationApi::class)

package com.dreamdisplays.core.protocol

import com.dreamdisplays.api.protocol.ProtocolVersion
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType
import java.util.*

/*
 * Protocol-v2 wire packets. These classes are the schema: the committed .proto artifact is
 * generated from them (`./gradlew :protocol:generateProto`, drift-checked by SchemaDriftTest).
 *
 * Compatibility rules: breaking any of these breaks old peers:
 *  - Never reuse or renumber a @ProtoNumber; only add new fields with fresh numbers.
 *  - Every field keeps a default value so missing/extra fields never fail decoding.
 *  - Type ids (see PacketRegistry) are append-only.
 */

/** Client -> server hello; replaces the legacy `version` packet and advertises capabilities. */
@Serializable
data class ClientHello(
    @ProtoNumber(1) val protocolVersion: Int = ProtocolVersion.CURRENT,
    @ProtoNumber(2) val modVersion: String = "",
    @ProtoNumber(3) val supportsPopout: Boolean = false,
    @ProtoNumber(4) val supportsHardwareDecode: Boolean = false,
    @ProtoNumber(5) val supportsHighResolution: Boolean = false,
    @ProtoNumber(6) val maxTextureSize: Int = 4096,
    @ProtoNumber(7) val supportedCodecs: List<String> = emptyList(),
    @ProtoNumber(8) val supportsPip: Boolean = false,
    @ProtoNumber(9) val supportsAudio: Boolean = true,
    @ProtoNumber(10) val renderBackend: String = "",
    @ProtoNumber(11) val shaderBackend: String = "",
    @ProtoNumber(12) val textureUploadPath: String = "",
    @ProtoNumber(13) val hwAccelBackend: String = "",
    @ProtoNumber(14) val nativeBackendAvailable: Boolean = false,
    @ProtoNumber(15) val nativeRgbaFramesEnabled: Boolean = false,
    @ProtoNumber(16) val nativeYuvGpuEnabled: Boolean = false,
    @ProtoNumber(17) val lavAvailable: Boolean = false,
    @ProtoNumber(18) val lavInProcessEnabled: Boolean = false,
    @ProtoNumber(19) val lavSurfaceInteropAvailable: Boolean = false,
    @ProtoNumber(20) val lavZeroCopyEnabled: Boolean = false,
    @ProtoNumber(21) val systemRamMb: Int = 0,
    @ProtoNumber(22) val maxJvmMemoryMb: Int = 0,
    @ProtoNumber(23) val dedicatedVramMb: Int = 0,
    @ProtoNumber(24) val warmDisplayLimit: Int = 0,
    @ProtoNumber(25) val nativeUnavailableReason: String = "",
    @ProtoNumber(26) val lavUnavailableReason: String = "",
    @ProtoNumber(27) val timeZoneOffsetMinutes: Int = 0,
) : DreamPacket

/** Server -> client capability snapshot (premium, admin, reporting); field 5 retired. */
@Serializable
data class ServerHello(
    @ProtoNumber(1) val protocolVersion: Int = ProtocolVersion.CURRENT,
    @ProtoNumber(2) val isPremium: Boolean = false,
    @ProtoNumber(3) val isAdmin: Boolean = false,
    @ProtoNumber(4) val isReportingEnabled: Boolean = false,
    @ProtoNumber(6) val maxDisplays: Int = -1,
    @ProtoNumber(7) val allowedFeatures: List<String> = emptyList(),
    @ProtoNumber(8) val defaultVolume: Float = -1f,
) : DreamPacket

/** Full description of a single display. */
@Serializable
data class DisplayInfo(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val ownerId: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(3) @ProtoType(ProtoIntegerType.SIGNED) val x: Int = 0,
    @ProtoNumber(4) @ProtoType(ProtoIntegerType.SIGNED) val y: Int = 0,
    @ProtoNumber(5) @ProtoType(ProtoIntegerType.SIGNED) val z: Int = 0,
    @ProtoNumber(6) val width: Int = 1,
    @ProtoNumber(7) val height: Int = 1,
    @ProtoNumber(8) val url: String = "",
    @ProtoNumber(9) val facing: Int = 0,
    @ProtoNumber(10) val isSync: Boolean = false,
    @ProtoNumber(11) val lang: String = "",
    @ProtoNumber(12) val isLocked: Boolean = true,
    @ProtoNumber(13) val mode: Int = 0,
    @ProtoNumber(14) val qualityCap: Int = 0,
    @ProtoNumber(15) val rotation: Int = 0,
    @ProtoNumber(16) val virtual: Boolean = false,
    @ProtoNumber(17) val forced: Boolean = false,
    @ProtoNumber(18) val scheduledStartEpochMillis: Long = 0,
    @ProtoNumber(19) val scheduledAction: Int = -1,
) : DreamPacket

/** Removes a display (server broadcast) or requests its deletion (client action). */
@Serializable
data class DisplayDelete(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
) : DreamPacket

/** Authoritative playback timeline for a display, pushed by server; [currentTimeMs] is position anchor. */
@Serializable
data class DisplaySync(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val isSync: Boolean = false,
    @ProtoNumber(3) val isPaused: Boolean = false,
    @ProtoNumber(4) val currentTimeMs: Long = 0,
    @ProtoNumber(5) val durationMs: Long = 0,
    @ProtoNumber(6) val serverTimeMs: Long = 0,
    @ProtoNumber(7) val loop: Boolean = false,
    @ProtoNumber(8) val mode: Int = 0,
) : DreamPacket

/** Client asks the server for the authoritative playback state of a display. */
@Serializable
data class RequestSync(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
) : DreamPacket

/** Client applies a new media URL / audio language to a display. */
@Serializable
data class SetVideo(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val url: String = "",
    @ProtoNumber(3) val lang: String = "",
) : DreamPacket

/** Client toggles the locked flag of a display it owns. */
@Serializable
data class SetLocked(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val locked: Boolean = true,
) : DreamPacket

/** Client reports a display to the server's configured webhook. */
@Serializable
data class ReportDisplay(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
) : DreamPacket

/** Display rendering toggle: bidirectional preference / admin control. */
@Serializable
data class SetDisplaysEnabled(
    @ProtoNumber(1) val enabled: Boolean = true,
) : DreamPacket

/** Server tells the client to evict the listed displays from local caches. */
@Serializable
data class ClearCache(
    @ProtoNumber(1) val ids: List<@Serializable(UuidSerializer::class) UUID> = emptyList(),
) : DreamPacket

/** Client playback intent for server-authoritative timeline (SYNCED or WATCH_PARTY). */
@Serializable
data class PlaybackCommand(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val action: Int = 0,
    @ProtoNumber(3) val positionMs: Long = 0,
) : DreamPacket

/** Client sets a display's persistent base mode; [mode] is a [PlaybackMode.wire] (not `WATCH_PARTY`). */
@Serializable
data class SetMode(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val mode: Int = 0,
    @ProtoNumber(3) val positionMs: Long = -1,
) : DreamPacket

/** Client starts a watch-party session on a display, becoming its host (display must be unlocked, or owner). */
@Serializable
data class WatchPartyStart(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val url: String = "",
    @ProtoNumber(3) val lang: String = "",
) : DreamPacket

/** Watch-party participant readiness or host control. */
@Serializable
data class WatchPartyControl(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val action: Int = 0,
    @ProtoNumber(3) val positionMs: Long = 0,
) : DreamPacket

/** Server snapshot of watch-party session, broadcast on every transition. */
@Serializable
data class WatchPartyState(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val sessionId: String = "",
    @ProtoNumber(3) val state: Int = 0,
    @ProtoNumber(4) val hostId: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(5) val hostName: String = "",
    @ProtoNumber(6) val url: String = "",
    @ProtoNumber(7) val lang: String = "",
    @ProtoNumber(8) val readyCount: Int = 0,
    @ProtoNumber(9) val nearbyCount: Int = 0,
    @ProtoNumber(10) val countdownStartEpochMs: Long = 0,
    @ProtoNumber(11) val positionMs: Long = 0,
    @ProtoNumber(12) val serverTimeMs: Long = 0,
    @ProtoNumber(13) val durationMs: Long = 0,
    @ProtoNumber(14) val paused: Boolean = true,
) : DreamPacket

/** Server snapshot of fullscreen broadcast targeting receiving player. */
@Serializable
data class FullscreenState(
    @ProtoNumber(1) val sessionId: String = "",
    @ProtoNumber(2) val displayId: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(3) val active: Boolean = false,
    @ProtoNumber(4) val mode: Int = 0,
    @ProtoNumber(5) val forced: Boolean = false,
    @ProtoNumber(6) val volume: Float = -1f,
    @ProtoNumber(7) val title: String = "",
    @ProtoNumber(8) val loop: Boolean = false,
    @ProtoNumber(9) val quality: String = "",
    @ProtoNumber(10) val minimized: Boolean = false,
) : DreamPacket

/** Client ack for [FullscreenState]: shown (0), dismissed (1), minimized (2). */
@Serializable
data class FullscreenAck(
    @ProtoNumber(1) val sessionId: String = "",
    @ProtoNumber(2) val action: Int = 0,
) : DreamPacket

/** Client pins/unpins display to PiP overlay; server persists per player. */
@Serializable
data class PipPin(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val pinned: Boolean = true,
) : DreamPacket

/** Server asks admin's client to render / hide fullscreen-broadcast radius preview. */
@Serializable
data class RadiusPreview(
    @ProtoNumber(1) val x: Double = 0.0,
    @ProtoNumber(2) val y: Double = 0.0,
    @ProtoNumber(3) val z: Double = 0.0,
    @ProtoNumber(4) val radius: Double = 0.0,
    @ProtoNumber(5) val show: Boolean = false,
    @ProtoNumber(6) val colorArgb: Int = 0,
) : DreamPacket

/** Client reports media duration after player initializes. */
@Serializable
data class ReportDuration(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val durationMs: Long = 0,
) : DreamPacket

/** Server tells client to pause / resume LOCAL-mode display player (no seek). */
@Serializable
data class RemotePlaybackToggle(
    @ProtoNumber(1) val id: @Serializable(UuidSerializer::class) UUID = ZERO_UUID,
    @ProtoNumber(2) val paused: Boolean = true,
) : DreamPacket
