package ru.l0sty.frogdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class RenderUtil2D {

    public static void drawTexturedQuad(MatrixStack matrices, int glId, float x, float y, float width, float height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, glId);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(matrix, x, y + height, 0.0F).color(255, 255, 255, 255).texture(0.0f, 1.0f);
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(255, 255, 255, 255).texture(1.0f, 1.0f);
        buffer.vertex(matrix, x + width, y, 0.0F).color(255, 255, 255, 255).texture(1.0f, 0.0f);
        buffer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).texture(0.0f, 0.0f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, 0);
    }


    public static void drawScaledTexture(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, texture);

        MatrixStack matrices = context.getMatrices();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        // Рисуем весь текстурный регион (UV 0.0 - 1.0)
        buffer.vertex(matrix, x, y + height, 0).texture(0f, 1f);
        buffer.vertex(matrix, x + width, y + height, 0).texture(1f, 1f);
        buffer.vertex(matrix, x + width, y, 0).texture(1f, 0f);
        buffer.vertex(matrix, x, y, 0).texture(0f, 0f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, 0);
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
