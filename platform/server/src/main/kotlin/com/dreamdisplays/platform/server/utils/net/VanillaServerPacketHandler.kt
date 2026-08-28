package com.dreamdisplays.platform.server.utils.net

import com.dreamdisplays.platform.client.Initializer
import com.dreamdisplays.platform.client.net.Packets
import com.dreamdisplays.platform.server.VanillaServerState
import com.dreamdisplays.platform.server.datatypes.sync.SyncData
import com.dreamdisplays.platform.server.managers.DisplayManager
import com.dreamdisplays.platform.server.managers.PlayerManager
import com.dreamdisplays.platform.server.managers.StateManager
import com.dreamdisplays.platform.server.utils.RegionUtil
import com.dreamdisplays.platform.server.utils.net.VanillaDisplayActions.delete
import com.dreamdisplays.platform.server.utils.net.VanillaDisplayActions.isAdmin
import com.dreamdisplays.platform.server.utils.net.VanillaDisplayActions.isPremium
import com.dreamdisplays.platform.server.utils.net.VanillaDisplayActions.recordVersionAndCheckUpdates
import com.dreamdisplays.platform.server.utils.net.VanillaDisplayActions.sendAllDisplays
import com.dreamdisplays.platform.server.utils.net.VanillaDisplayActions.setLocked
import com.dreamdisplays.platform.server.utils.net.VanillaDisplayActions.setVideo
import io.github.arnodoelinger.platformweaver.FabricOnly
import io.github.arnodoelinger.platformweaver.NeoForgeOnly
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import org.slf4j.LoggerFactory

/**
 * Registers the frozen protocol-v1 packet receivers for `Fabric` and `NeoForge`. Business logic is
 * shared by both loaders through [VanillaDisplayActions]; only the two [registerReceivers] overloads
 * here are loader-specific, since Fabric's and NeoForge's payload-registration APIs are unrelated.
 */
object VanillaServerPacketHandler {
    /** Logger. */
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Runs [block], logging (and swallowing) any failure as `"Failed to handle $packetName packet."`. */
    private inline fun runLogged(packetName: String, block: () -> Unit) {
        runCatching(block).onFailure { e -> logger.warn("Failed to handle $packetName packet.", e) }
    }

    /** Registers all frozen v1 packet receivers for `Fabric` servers. */
    @FabricOnly
    @Deprecated("Protocol v1 receivers; remove when v1 client support is dropped.")
    fun registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(Packets.Version.PACKET_ID) { payload, context ->
            handleVersionPacket(context.player(), context.server(), payload.version)
        }

        ServerPlayNetworking.registerGlobalReceiver(Packets.Sync.PACKET_ID) { payload, context ->
            handleSyncPacket(context.player(), context.server(), payload)
        }

        ServerPlayNetworking.registerGlobalReceiver(Packets.RequestSync.PACKET_ID) { payload, context ->
            runLogged("request_sync") {
                StateManager.sendSyncPacket(payload.uuid, context.player())
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(Packets.Delete.PACKET_ID) { payload, context ->
            runLogged("delete") {
                delete(context.player(), context.server(), payload.uuid)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(Packets.Report.PACKET_ID) { payload, context ->
            runLogged("report") {
                DisplayManager.report(payload.uuid, context.player(), context.server())
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(Packets.DisplayEnabled.PACKET_ID) { payload, context ->
            runLogged("display_enabled") {
                PlayerManager.setDisplaysEnabled(context.player().uuid, payload.enabled)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(Packets.SetVideo.PACKET_ID) { payload, context ->
            runLogged("set_video") {
                setVideo(context.player(), context.server(), payload.uuid, payload.url, payload.lang)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(Packets.SetLocked.PACKET_ID) { payload, context ->
            runLogged("set_locked") {
                setLocked(context.player(), context.server(), payload.uuid, payload.locked)
            }
        }
    }

    /**
     * Registers all frozen v1 packets against [registrar]. Must be called exactly once total for
     * the whole mod (`NeoForge`'s payload registry rejects a second registration of the same id) —
     * see `NeoForgeServer.registerPayloads` for why this can't also be registered from `Client`.
     */
    @NeoForgeOnly
    @Deprecated("Protocol v1 receivers; remove when v1 client support is dropped.")
    fun registerReceivers(registrar: PayloadRegistrar) {
        registrar.playToServer(Packets.Version.PACKET_ID, Packets.Version.PACKET_CODEC) { payload, context ->
            val player = context.player() as ServerPlayer
            handleVersionPacket(player, RegionUtil.playerServer(player), payload.version)
        }

        registrar.playBidirectionalCompat(
            Packets.Sync.PACKET_ID, Packets.Sync.PACKET_CODEC,
            { payload, context ->
                val player = context.player() as ServerPlayer
                handleSyncPacket(player, RegionUtil.playerServer(player), payload)
            },
            clientHandler { payload, _ -> Initializer.onLegacyPacket(payload) },
        )

        registrar.playToServer(Packets.RequestSync.PACKET_ID, Packets.RequestSync.PACKET_CODEC) { payload, context ->
            runLogged("request_sync") {
                StateManager.sendSyncPacket(payload.uuid, context.player() as ServerPlayer)
            }
        }

        registrar.playBidirectionalCompat(
            Packets.Delete.PACKET_ID, Packets.Delete.PACKET_CODEC,
            { payload, context ->
                runLogged("delete") {
                    val player = context.player() as ServerPlayer
                    delete(player, RegionUtil.playerServer(player), payload.uuid)
                }
            },
            clientHandler { payload, _ -> Initializer.onLegacyPacket(payload) },
        )

        registrar.playToServer(Packets.Report.PACKET_ID, Packets.Report.PACKET_CODEC) { payload, context ->
            runLogged("report") {
                val player = context.player() as ServerPlayer
                DisplayManager.report(payload.uuid, player, RegionUtil.playerServer(player))
            }
        }

        registrar.playBidirectionalCompat(
            Packets.DisplayEnabled.PACKET_ID, Packets.DisplayEnabled.PACKET_CODEC,
            { payload, context ->
                runLogged("display_enabled") {
                    PlayerManager.setDisplaysEnabled((context.player() as ServerPlayer).uuid, payload.enabled)
                }
            },
            clientHandler { payload, _ -> Initializer.onLegacyPacket(payload) },
        )

        registrar.playToServer(Packets.SetVideo.PACKET_ID, Packets.SetVideo.PACKET_CODEC) { payload, context ->
            runLogged("set_video") {
                val player = context.player() as ServerPlayer
                setVideo(player, RegionUtil.playerServer(player), payload.uuid, payload.url, payload.lang)
            }
        }

        registrar.playToServer(Packets.SetLocked.PACKET_ID, Packets.SetLocked.PACKET_CODEC) { payload, context ->
            runLogged("set_locked") {
                val player = context.player() as ServerPlayer
                setLocked(player, RegionUtil.playerServer(player), payload.uuid, payload.locked)
            }
        }

        registrar.playToClient(Packets.Info.PACKET_ID, Packets.Info.PACKET_CODEC) { payload, _ ->
            Initializer.onLegacyPacket(payload)
        }
        registrar.playToClient(Packets.Premium.PACKET_ID, Packets.Premium.PACKET_CODEC) { payload, _ ->
            Initializer.onLegacyPacket(payload)
        }
        registrar.playToClient(Packets.IsAdmin.PACKET_ID, Packets.IsAdmin.PACKET_CODEC) { payload, _ ->
            Initializer.onLegacyPacket(payload)
        }
        registrar.playToClient(Packets.ReportEnabled.PACKET_ID, Packets.ReportEnabled.PACKET_CODEC) { payload, _ ->
            Initializer.onLegacyPacket(payload)
        }
        registrar.playToClient(Packets.ClearCache.PACKET_ID, Packets.ClearCache.PACKET_CODEC) { payload, _ ->
            Initializer.onLegacyPacket(payload)
        }
    }

    private fun handleVersionPacket(player: ServerPlayer, server: MinecraftServer, version: String) {
        runCatching {
            if (V2PlayerTracker.isV2(player.uuid)) return@runCatching

            recordVersionAndCheckUpdates(player, version)
            VanillaPacketUtil.sendPremium(player, isPremium(player))
            VanillaPacketUtil.sendIsAdmin(player, isAdmin(player))
            VanillaPacketUtil.sendReportEnabled(player, VanillaServerState.config.settings.webhookUrl.isNotEmpty())
            sendAllDisplays(player, server)
        }.onFailure { e ->
            logger.warn("Failed to process version packet.", e)
        }
    }

    private fun handleSyncPacket(player: ServerPlayer, server: MinecraftServer, payload: Packets.Sync) {
        val syncData = SyncData(
            id = payload.uuid,
            isSync = payload.isSync,
            currentState = payload.currentState,
            currentTime = payload.currentTime,
            limitTime = payload.limitTime
        )
        runCatching {
            StateManager.processSyncPacket(syncData, player, server, isAdmin(player))
        }.onFailure { e ->
            logger.warn("Failed to handle sync packet.", e)
        }
    }
}
