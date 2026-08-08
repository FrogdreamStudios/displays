package com.dreamdisplays.platform.proxy.bungee

import com.dreamdisplays.core.protocol.proxy.ApplyNetworkWatchParty
import com.dreamdisplays.core.protocol.proxy.BackendHello
import com.dreamdisplays.core.protocol.proxy.ClockProbe
import com.dreamdisplays.core.protocol.proxy.ClockReply
import com.dreamdisplays.core.protocol.proxy.CloseNetworkWatchParty
import com.dreamdisplays.core.protocol.proxy.DisplayTokenResolved
import com.dreamdisplays.core.protocol.proxy.ListNetworkSessions
import com.dreamdisplays.core.protocol.proxy.NetworkFullscreenAck
import com.dreamdisplays.core.protocol.proxy.NetworkSessionList
import com.dreamdisplays.core.protocol.proxy.NetworkWatchPartyState
import com.dreamdisplays.core.protocol.proxy.PlayerLeftNetwork
import com.dreamdisplays.core.protocol.proxy.PlayerReady
import com.dreamdisplays.core.protocol.proxy.PlayerTransferring
import com.dreamdisplays.core.protocol.proxy.ProxyPacket
import com.dreamdisplays.core.protocol.proxy.ProxyPacketRegistry
import com.dreamdisplays.core.protocol.proxy.ProxyWelcome
import com.dreamdisplays.core.protocol.proxy.ReplayForPlayer
import com.dreamdisplays.core.protocol.proxy.ResolveDisplayToken
import com.dreamdisplays.core.protocol.proxy.StartNetworkFullscreen
import com.dreamdisplays.core.protocol.proxy.StartNetworkWatchParty
import com.dreamdisplays.core.protocol.proxy.StopNetworkFullscreen
import com.dreamdisplays.platform.proxy.BungeeOnly
import com.dreamdisplays.platform.proxy.NetworkBackendRegistry
import com.dreamdisplays.platform.proxy.NetworkFullscreenManager
import com.dreamdisplays.platform.proxy.NetworkWatchPartyManager
import net.md_5.bungee.api.connection.Server
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler

/** `dreamdisplays:proxy` channel tag, shared verbatim with the Velocity sibling (see that plugin's doc). */
private const val PROXY_CHANNEL = "dreamdisplays:proxy"

/**
 * `BungeeCord` entry point for the thin-coordinator proxy plugin.
 *
 * Same responsibilities as [com.dreamdisplays.platform.proxy.velocity.DreamDisplaysVelocity]:
 * register the `dreamdisplays:proxy` plugin-message channel, track the configured backend roster,
 * and answer each backend's [BackendHello] with a [ProxyWelcome] naming it.
 */
@BungeeOnly
class DreamDisplaysBungee : Plugin(), Listener {
    override fun onEnable() {
        proxy.registerChannel(PROXY_CHANNEL)
        proxy.pluginManager.registerListener(this, this)
        refreshKnownServers()
        logger.info(
            "DreamDisplays proxy bridge ready on BungeeCord - " +
                    "${proxy.servers.size} backend(s) configured: ${NetworkBackendRegistry.allServerNames().sorted()}"
        )
    }

    /** `Bungee`'s server roster is config-driven and static at runtime, unlike `Velocity`'s — resynced on enable and on every hello for consistency with the `Velocity` sibling. */
    private fun refreshKnownServers() {
        NetworkBackendRegistry.updateKnownServers(proxy.servers.keys)
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.tag != PROXY_CHANNEL) return
        val sender = event.sender as? Server ?: return
        event.isCancelled = true

        val packet = runCatching { ProxyPacketRegistry.decode(event.data) }.getOrNull() ?: return
        val serverName = sender.info.name
        when (packet) {
            is BackendHello -> {
                refreshKnownServers()
                NetworkBackendRegistry.recordHello(serverName, packet, System.currentTimeMillis())
                logger.info(
                    "Backend '$serverName' announced itself (Dream Displays ${packet.pluginVersion}, " +
                            "MC ${packet.mcVersion}, ${packet.platform})"
                )
                val welcome = ProxyWelcome(
                    yourServerName = serverName,
                    allServerNames = NetworkBackendRegistry.allServerNames().toList(),
                    proxyNowMs = System.currentTimeMillis(),
                )
                sender.sendData(PROXY_CHANNEL, ProxyPacketRegistry.encode(welcome))
                retryPendingSessions(serverName)
            }

            is ClockProbe -> {
                val proxyRecvMs = System.currentTimeMillis()
                val reply = ClockReply(
                    backendSendMs = packet.backendSendMs,
                    proxyRecvMs = proxyRecvMs,
                    proxySendMs = System.currentTimeMillis(),
                )
                sender.sendData(PROXY_CHANNEL, ProxyPacketRegistry.encode(reply))
            }

            is StartNetworkFullscreen -> {
                val session = NetworkFullscreenManager.start(packet, System.currentTimeMillis())
                val targets = NetworkFullscreenManager.targetServers(session.scope, NetworkBackendRegistry.allServerNames())
                if (targets.isEmpty()) {
                    logger.warning("Network fullscreen '${session.sessionId}' from '$serverName' matched no backends for scope '${session.scope}'")
                }
                NetworkFullscreenManager.markPending(session.sessionId, targets)
                val apply = NetworkFullscreenManager.toApplyPacket(session)
                targets.forEach { name -> sendTo(name, apply) }
            }

            is NetworkFullscreenAck -> NetworkFullscreenManager.onAck(packet.sessionId, serverName, packet.reach, packet.pending)

            is StopNetworkFullscreen -> {
                val session = NetworkFullscreenManager.stop(packet.sessionId)
                val targets = session?.let { NetworkFullscreenManager.targetServers(it.scope, NetworkBackendRegistry.allServerNames()) }
                    ?: NetworkBackendRegistry.allServerNames()
                targets.forEach { name -> sendTo(name, packet) }
            }

            is ListNetworkSessions ->
                sender.sendData(PROXY_CHANNEL, ProxyPacketRegistry.encode(NetworkSessionList(NetworkFullscreenManager.list())))

            is PlayerReady -> {
                retryPendingSessions(serverName)
                val applicable = NetworkFullscreenManager.sessionIdsApplicableTo(serverName, NetworkBackendRegistry.allServerNames())
                sendTo(serverName, ReplayForPlayer(packet.playerId, applicable))
            }

            is StartNetworkWatchParty -> {
                val party = NetworkWatchPartyManager.start(packet, hostServer = serverName)
                sendTo(
                    serverName,
                    ApplyNetworkWatchParty(party.partyId, party.sharedDisplayId.toString(), party.hostId, party.url, party.lang),
                )
            }

            is NetworkWatchPartyState ->
                NetworkWatchPartyManager.relayTargets(packet.partyId).forEach { name -> sendTo(name, packet) }

            is CloseNetworkWatchParty -> {
                val targets = NetworkWatchPartyManager.relayTargets(packet.partyId)
                NetworkWatchPartyManager.stop(packet.partyId)
                targets.forEach { name -> sendTo(name, packet) }
            }

            is ResolveDisplayToken -> {
                val stamped = packet.copy(originServer = serverName)
                (NetworkBackendRegistry.allServerNames() - serverName).forEach { name -> sendTo(name, stamped) }
            }

            is DisplayTokenResolved -> sendTo(packet.originServer, packet)

            else -> logger.fine("Unhandled proxy packet from '$serverName': $packet")
        }
    }

    /**
     * Fires while the player's connection to their current backend is still open, before the switch
     * actually happens — tells that backend "this quit is a transfer" so it doesn't start any
     * host-disconnect grace timer for them. A same-server reconnect is not a transfer, so it's excluded.
     */
    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        val player = event.player
        val fromServer = player.server?.info?.name ?: return
        val toServer = event.target.name
        if (fromServer == toServer) return
        sendTo(fromServer, PlayerTransferring(player.uniqueId.toString(), fromServer, toServer))
    }

    /**
     * Fires on a whole-proxy disconnect (not a backend switch — that's [onServerConnect] followed by
     * a normal connect, with no [PlayerDisconnectEvent] in between). Clears any [PlayerTransferring]
     * mark the last backend might still be holding, so an interrupted transfer attempt can't mask a
     * later real quit for the rest of its TTL.
     */
    @EventHandler
    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        val player = event.player
        val lastServer = player.server?.info?.name ?: return
        sendTo(lastServer, PlayerLeftNetwork(player.uniqueId.toString(), lastServer))
    }

    /** Sends [packet] to backend [serverName], if it's currently registered on this proxy. */
    private fun sendTo(serverName: String, packet: ProxyPacket) {
        proxy.getServerInfo(serverName)?.sendData(PROXY_CHANNEL, ProxyPacketRegistry.encode(packet))
    }

    /** Re-applies every network fullscreen session [serverName] is still owed, after it (re)announces itself. */
    private fun retryPendingSessions(serverName: String) {
        NetworkFullscreenManager.pendingSessionsFor(serverName).forEach { session ->
            sendTo(serverName, NetworkFullscreenManager.toApplyPacket(session))
        }
    }
}
