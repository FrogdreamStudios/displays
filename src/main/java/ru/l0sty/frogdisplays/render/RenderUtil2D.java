package ru.l0sty.frogdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class RenderUtil2D {

    /// Fixes the rotation of the matrix stack based on the facing direction.
    public static void drawTexturedQuad(MatrixStack matrices, GpuTexture glId, float x, float y, float width, float height, RenderLayer layer) {
        RenderSystem.setShaderTexture(0, glId);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL
        );

        buffer
                .vertex(matrix, x, y + height, 0.0F)
                .color(255, 255, 255, 255)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .texture(0.0f, 1.0f);
        buffer
                .vertex(matrix, x + width, y + height, 0.0F)
                .color(255, 255, 255, 255)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .texture(1.0f, 1.0f);
        buffer
                .vertex(matrix, x + width, y, 0.0F)
                .color(255, 255, 255, 255)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .texture(1.0f, 0.0f);
        buffer
                .vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .texture(0.0f, 0.0f);

        layer.draw(buffer.end());
    }
}
