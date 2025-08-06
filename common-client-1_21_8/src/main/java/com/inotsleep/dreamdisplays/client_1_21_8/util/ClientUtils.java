package com.inotsleep.dreamdisplays.client_1_21_8.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;

public class ClientUtils {

    public static BlockPos rayCast(double maxDistance) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null)
            return null;

        Vec3 start = client.player.getEyePosition(1.0f);
        Vec3 direction = client.player.getLookAngle();
        Vec3 end = start.add(direction.multiply(new Vec3(maxDistance, maxDistance, maxDistance)));

        BlockHitResult hit = client.level.clip(new ClipContext(
            start,
            end,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            client.player
        ));

        if (hit.getType() == BlockHitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }

        return null;
    }

    public static Path getClientSettingSavePath() {
        Minecraft client = Minecraft.getInstance();
        Path gameDir = client.gameDirectory.toPath();

        ClientPacketListener networkHandler = client.getConnection();
        if (networkHandler != null) {
            Connection conn = networkHandler.getConnection();
            SocketAddress addr = conn.getRemoteAddress();

            String id;
            if (addr instanceof InetSocketAddress inet) {
                id = inet.getHostString() + "_" + inet.getPort();
            } else {
                id = addr.toString().replaceAll("[^A-Za-z0-9_\\-]", "_");
            }

            return gameDir
                    .resolve("dreamdisplays")
                    .resolve(id)
                    .resolve("display-settings.yml");
        }

        IntegratedServer server = client.getSingleplayerServer();
        if (server != null) {
            return server
                    .getWorldPath(LevelResource.ROOT)
                    .resolve("display-settings.yml");
        }

        return null;
    }
}