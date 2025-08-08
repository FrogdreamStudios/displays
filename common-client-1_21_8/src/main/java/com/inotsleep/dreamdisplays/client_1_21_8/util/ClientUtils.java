package com.inotsleep.dreamdisplays.client_1_21_8.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
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

    public static void renderGpuTexture(PoseStack poseStack, GpuTextureView gpuTextureView, RenderType renderType) {
        RenderSystem.setShaderTexture(0, gpuTextureView);
        Matrix4f mat = poseStack.last().pose();

        BufferBuilder buf = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );

        buf
                .addVertex(mat, 0f, 0f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, 1f)
                .setNormal(0f, 0f, 1f);

        buf
                .addVertex(mat, 1f, 0f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, 1f)
                .setNormal(0f, 0f, 1f);

        buf
                .addVertex(mat, 1f, 1f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, 0f)
                .setNormal(0f, 0f, 1f);

        buf
                .addVertex(mat, 0f, 1f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, 0f)
                .setNormal(0f, 0f, 1f);

        MeshData mesh = buf.build();
        renderType.draw(mesh);

    }

    public static void renderColor(PoseStack poseStack, int r, int g, int b, RenderType renderType) {
        Matrix4f mat = poseStack.last().pose();

        BufferBuilder buf = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );

        buf
                .addVertex(mat, 0f, 0f, 0f)
                .setColor(r, g, b, 255)
                .setUv(0f, 1f)
                .setNormal(0f, 0f, 1f);

        buf
                .addVertex(mat, 1f, 0f, 0f)
                .setColor(r, g, b, 255)
                .setUv(1f, 1f)
                .setNormal(0f, 0f, 1f);

        buf
                .addVertex(mat, 1f, 1f, 0f)
                .setColor(r, g, b, 255)
                .setUv(1f, 0f)
                .setNormal(0f, 0f, 1f);

        buf
                .addVertex(mat, 0f, 1f, 0f)
                .setColor(r, g, b, 255)
                .setUv(0f, 0f)
                .setNormal(0f, 0f, 1f);

        MeshData mesh = buf.build();
        renderType.draw(mesh);

    }
}