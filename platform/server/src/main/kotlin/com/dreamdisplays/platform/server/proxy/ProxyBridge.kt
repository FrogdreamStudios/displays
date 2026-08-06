package com.dreamdisplays.platform.server.proxy

import com.dreamdisplays.api.playback.FullscreenMode
import com.dreamdisplays.core.protocol.proxy.ApplyFullscreen
import com.dreamdisplays.core.protocol.proxy.BackendHello
import com.dreamdisplays.core.protocol.proxy.ClockProbe
import com.dreamdisplays.core.protocol.proxy.ClockReply
import com.dreamdisplays.core.protocol.proxy.ListNetworkSessions
import com.dreamdisplays.core.protocol.proxy.NetworkFullscreenAck
import com.dreamdisplays.core.protocol.proxy.NetworkSessionList
import com.dreamdisplays.core.protocol.proxy.PlayerLeftNetwork
import com.dreamdisplays.core.protocol.proxy.PlayerReady
import com.dreamdisplays.core.protocol.proxy.PlayerTransferring
import com.dreamdisplays.core.protocol.proxy.ProxyPacket
import com.dreamdisplays.core.protocol.proxy.ProxyPacketDirection
import com.dreamdisplays.core.protocol.proxy.ProxyPacketRegistry
import com.dreamdisplays.core.protocol.proxy.ProxyWelcome
import com.dreamdisplays.core.protocol.proxy.ReplayForPlayer
import com.dreamdisplays.core.protocol.proxy.StartNetworkFullscreen
import com.dreamdisplays.core.protocol.proxy.StopNetworkFullscreen
import com.dreamdisplays.platform.server.PaperServer
import com.dreamdisplays.platform.server.playback.FullscreenBroadcastManager
import io.github.arnodoelinger.platformweaver.PaperOnly
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.jspecify.annotations.NullMarked
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** The single `dreamdisplays:proxy` plugin-message channel, backend<->proxy only. */
const val PROXY_CHANNEL: String = "dreamdisplays:proxy"

/**
 * Backend side of the thin-coordinator proxy bridge.
 */
@PaperOnly
@NullMarked
object ProxyBridge : PluginMessageListener {
    private val logger = LoggerFactory.getLogger("DreamDisplays/ProxyBridge")
    private val plugin: PaperServer by lazy { PaperServer.getInstance() }
    private val helloSent = AtomicBoolean(false)
    private val lastClockProbeMs = AtomicLong(0L)

    /** Whether the `[proxy]` section is enabled in the active config; gates channel registration. */
    var enabled: Boolean = false
        private set

    /** `[proxy] clock_sync_interval`, in milliseconds; how often [tick] emits a [ClockProbe]. */
    private var clockSyncIntervalMs: Long = 5_000L

    /** Called from [com.dreamdisplays.platform.server.PaperServer.doEnable]. Resets per-session state. */
    fun init(enabled: Boolean, clockSyncIntervalSeconds: Int = 5) {
        this.enabled = enabled
        this.clockSyncIntervalMs = clockSyncIntervalSeconds.coerceAtLeast(1) * 1_000L
        helloSent.set(false)
        ProxyClock.reset()
    }

    /**
     * Called from the join handshake once a player is fully v2-negotiated — on every join and
     * every proxy-driven server switch, not just this backend's first-ever player. Announces the
     * backend itself once per restart ([BackendHello]), then always reports this specific player as
     * ready so the proxy can [ReplayForPlayer] any network state they should still see here.
     */
    fun onPlayerReady(player: Player) {
        if (!enabled) return
        if (helloSent.compareAndSet(false, true)) {
            send(
                player,
                BackendHello(
                    pluginVersion = plugin.description.version,
                    mcVersion = Bukkit.getMinecraftVersion(),
                    platform = "paper",
                ),
            )
        }
        send(player, PlayerReady(playerId = player.uniqueId.toString()))
    }

    /** Encodes and sends [packet] on [PROXY_CHANNEL] via [player]'s connection to the proxy. */
    private fun send(player: Player, packet: ProxyPacket) {
        val bytes = runCatching { ProxyPacketRegistry.encode(packet) }
            .onFailure { logger.warn("Failed to encode proxy packet", it) }
            .getOrNull() ?: return
        runCatching { player.sendPluginMessage(plugin, PROXY_CHANNEL, bytes) }
            .onFailure { logger.debug("Failed to send proxy packet (no proxy listening?)", it) }
    }

    /** Same as [send], but picks any online player to ride the message — for packets with no natural sender. */
    private fun sendViaAnyPlayer(packet: ProxyPacket) {
        val rider = Bukkit.getOnlinePlayers().firstOrNull() ?: return
        send(rider, packet)
    }

    /**
     * Forwards `/display fullscreen start server <scope> ...` to the proxy for network-wide fan-out.
     * Called from [com.dreamdisplays.platform.server.commands.subcommands.FullscreenCommand] instead
     * of [FullscreenBroadcastManager.start] when the command carried a `server` scope.
     */
    fun startNetworkFullscreen(
        player: Player,
        scope: String,
        url: String,
        mode: FullscreenMode?,
        forced: Boolean,
        volume: Float?,
        loop: Boolean,
        quality: String?,
    ) {
        send(
            player,
            StartNetworkFullscreen(
                scope = scope,
                ownerId = player.uniqueId.toString(),
                url = url,
                mode = (mode ?: FullscreenMode.STANDARD).ordinal,
                forced = forced,
                volume = volume ?: -1f,
                loop = loop,
                quality = quality ?: "",
            ),
        )
    }

    /** Forwards `/display fullscreen stop <id>` to the proxy when [sessionId] isn't a live local session. */
    fun stopNetworkFullscreen(player: Player, sessionId: String) {
        send(player, StopNetworkFullscreen(sessionId))
    }

    /** Requests a fresh [NetworkSessionList], refreshing [ProxyNetwork.networkSessions] for the next `/display fullscreen list`. */
    fun requestNetworkSessions(player: Player) {
        send(player, ListNetworkSessions())
    }

    /** Applies a proxy-driven [ApplyFullscreen], creating a fresh local virtual display for it. */
    private fun applyNetworkFullscreen(packet: ApplyFullscreen) {
        val ownerId = runCatching { UUID.fromString(packet.ownerId) }.getOrNull() ?: return
        val resolved = FullscreenBroadcastManager.resolveOrCreateDisplay(packet.url, ownerId)
        if (resolved == null) {
            logger.warn("Could not create a virtual display for network fullscreen '{}' (no world loaded yet?)", packet.sessionId)
            sendViaAnyPlayer(NetworkFullscreenAck(packet.sessionId, reach = 0, pending = true))
            return
        }
        val (display, _) = resolved
        val onlineIds = Bukkit.getOnlinePlayers().map { it.uniqueId }.toSet()
        val mode = FullscreenMode.entries.getOrElse(packet.mode) { FullscreenMode.STANDARD }
        val sessionId = FullscreenBroadcastManager.start(
            sessionId = packet.sessionId,
            display = display,
            virtual = true,
            transientSession = true,
            ownerId = ownerId,
            mode = mode,
            forced = packet.forced,
            volume = packet.volume,
            loop = packet.loop,
            quality = packet.quality,
            title = packet.title,
            namedTargets = onlineIds,
            radius = null,
            timelineAnchorMs = ProxyClock.toLocal(packet.anchorProxyMs),
        )
        val reach = if (sessionId != null) {
            FullscreenBroadcastManager.list().firstOrNull { it.sessionId == sessionId }?.reach ?: 0
        } else 0
        sendViaAnyPlayer(NetworkFullscreenAck(packet.sessionId, reach = reach, pending = sessionId == null))
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != PROXY_CHANNEL) return
        val packet = runCatching { ProxyPacketRegistry.decode(message, ProxyPacketDirection.PROXY_TO_BACKEND) }
            .onFailure { logger.warn("Failed to decode proxy packet", it) }
            .getOrNull() ?: return

        when (packet) {
            is ProxyWelcome -> {
                ProxyNetwork.update(packet.yourServerName, packet.allServerNames)
                logger.info(
                    "Proxy welcomed this backend as '{}' - network has {} server(s): {}",
                    packet.yourServerName, packet.allServerNames.size, packet.allServerNames,
                )
            }

            is ClockReply -> {
                val now = System.currentTimeMillis()
                ProxyClock.onClockReply(packet, now)
                logger.debug("Clock offset now ~{} ms (proxy - local)", ProxyClock.proxyNow() - now)
            }

            is ApplyFullscreen -> applyNetworkFullscreen(packet)

            is StopNetworkFullscreen -> FullscreenBroadcastManager.stop(packet.sessionId)

            is NetworkSessionList -> ProxyNetwork.updateSessions(packet.sessions)

            is ReplayForPlayer -> {
                val uuid = runCatching { UUID.fromString(packet.playerId) }.getOrNull() ?: return
                packet.sessionIds.forEach { sessionId -> FullscreenBroadcastManager.addTarget(sessionId, uuid) }
            }

            is PlayerTransferring -> {
                val uuid = runCatching { UUID.fromString(packet.playerId) }.getOrNull() ?: return
                TransferTracker.markTransferring(uuid)
            }

            is PlayerLeftNetwork -> {
                val uuid = runCatching { UUID.fromString(packet.playerId) }.getOrNull() ?: return
                TransferTracker.clear(uuid)
            }

            else -> logger.debug("Ignoring non-backend-bound proxy packet {}.", packet::class.simpleName)
        }
    }

    /** Per-tick upkeep: emits a [ClockProbe] every `clock_sync_interval` seconds, once connected. */
    fun tick() {
        if (!enabled || !helloSent.get()) return
        val now = System.currentTimeMillis()
        if (now - lastClockProbeMs.get() < clockSyncIntervalMs) return
        val rider = Bukkit.getOnlinePlayers().firstOrNull() ?: return
        lastClockProbeMs.set(now)
        send(rider, ClockProbe(backendSendMs = now))
    }
}
