package com.inotsleep.dreamdisplays.managers;

import com.inotsleep.dreamdisplays.datatypes.DisplayData;
import com.inotsleep.dreamdisplays.datatypes.PlayState;
import com.inotsleep.dreamdisplays.utils.net.PacketUtils;
import com.inotsleep.dreamdisplays.datatypes.SyncPacket;
import me.inotsleep.utils.logging.LoggingManager;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayStateManager {
    private static final Map<UUID, PlayState> playStates = new HashMap<>();

    public static void processSyncPacket(SyncPacket packet, Player player) {
        DisplayData data = DisplayManager.getDisplayData(packet.getId());
        if (data != null) data.setSync(packet.isSync());

        if (!packet.isSync()) {
            playStates.remove(packet.getId());
            return;
        }

        if (data == null) return;

        if (!(data.getOwnerId() + "").equals(player.getUniqueId() + "")) {
            LoggingManager.warn("Player " + player.getName() + " sent sync packet while he not owner! ");
            return;
        }

        PlayState state = playStates.computeIfAbsent(packet.getId(), PlayState::new);
        state.update(packet);
        data.setDuration(packet.getLimitTime());
        List<Player> receivers = data.getReceivers();

        PacketUtils.sendSyncPacket(receivers.stream().filter(p -> !p.getUniqueId().equals(player.getUniqueId())).toList(), packet);
    }

    public static void sendSyncPacket(UUID id, Player player) {
        if (!playStates.containsKey(id)) return;
        PlayState state = playStates.get(id);

        SyncPacket packet = state.createPacket();
        PacketUtils.sendSyncPacket(Collections.singletonList(player), packet);
    }
}