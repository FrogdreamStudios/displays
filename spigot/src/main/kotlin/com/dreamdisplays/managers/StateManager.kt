package com.dreamdisplays.managers

import com.dreamdisplays.datatypes.StateData
import com.dreamdisplays.datatypes.SyncData
import com.dreamdisplays.managers.DisplayManager.getDisplayData
import com.dreamdisplays.managers.DisplayManager.getReceivers
import com.dreamdisplays.utils.net.Utils
import me.inotsleep.utils.logging.LoggingManager
import org.bukkit.entity.Player
import org.jspecify.annotations.NullMarked
import java.util.*

@NullMarked
object StateManager {
    private val playStates: MutableMap<UUID?, StateData> = HashMap<UUID?, StateData>()

    @JvmStatic
    fun processSyncPacket(packet: SyncData, player: Player) {
        val data = getDisplayData(packet.id)
        if (data != null) data.isSync = packet.isSync

        if (!packet.isSync) {
            playStates.remove(packet.id)
            return
        }

        if (data == null) return

        if (data.ownerId != player.uniqueId) {
            LoggingManager.warn("Player " + player.name + " sent sync packet while he not owner! ")
            return
        }

        val state = playStates.computeIfAbsent(packet.id) { id: UUID? -> StateData(id) }
        state.update(packet)
        data.duration = packet.limitTime

        val receivers = getReceivers(data)

        Utils.sendSyncPacket(receivers.filter { it.uniqueId != player.uniqueId }.toMutableList(), packet)
    }

    @JvmStatic
    fun sendSyncPacket(id: UUID?, player: Player?) {
        val state = playStates[id] ?: return

        val packet = state.createPacket()
        Utils.sendSyncPacket(mutableListOf(player), packet)
    }
}
