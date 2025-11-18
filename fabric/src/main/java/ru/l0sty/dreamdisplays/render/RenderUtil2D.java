package ru.l0sty.dreamdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Utility class for rendering 2D textures in Minecraft.
 * This class provides methods to draw textured quads using the specified texture and render layer.
 */
public class RenderUtil2D {
    public static void drawTexturedQuad(PoseStack matrices, GpuTexture glId, float x, float y, float width, float height, RenderType layer) {
        RenderSystem.setShaderTexture(0, glId);

        Matrix4f matrix = matrices.last().pose();

        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.BLOCK
        );

        buffer
                .addVertex(matrix, x, y + height, 0.0F)
                .setColor(255, 255, 255, 255)
                .setLight(0xF000F0)
                .setNormal(0f, 0f, 1f)
                .setUv(0.0f, 1.0f);
        buffer
                .addVertex(matrix, x + width, y + height, 0.0F)
                .setColor(255, 255, 255, 255)
                .setLight(0xF000F0)
                .setNormal(0f, 0f, 1f)
                .setUv(1.0f, 1.0f);
        buffer
                .addVertex(matrix, x + width, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setLight(0xF000F0)
                .setNormal(0f, 0f, 1f)
                .setUv(1.0f, 0.0f);
        buffer
                .addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setLight(0xF000F0)
                .setNormal(0f, 0f, 1f)
                .setUv(0.0f, 0.0f);

        layer.draw(buffer.buildOrThrow());
    }
}