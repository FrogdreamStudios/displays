package ru.l0sty.frogdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class RenderUtil2D {

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

    public static int createEmptyTexture(int width, int height) {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Настройки фильтрации
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // Пустой буфер (черный кадр)
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4); // RGBA = 4 байта на пиксель

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        return textureId;
    }

}
