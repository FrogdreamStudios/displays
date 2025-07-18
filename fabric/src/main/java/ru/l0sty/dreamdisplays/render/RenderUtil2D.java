package ru.l0sty.dreamdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector2f;

/**
 * Utility class for rendering 2D textures in Minecraft.
 * This class provides methods to draw textured quads using the specified texture and render layer.
 */
public class RenderUtil2D {
    public static void drawTexturedQuad(Matrix3x2fStack matrices, GpuTextureView glId, float x, float y, float width, float height, RenderLayer layer) {
        RenderSystem.setShaderTexture(0, glId);

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL
        );

        // 4) Трансформируем четыре угла
        Vector2f p1 = new Vector2f(), p2 = new Vector2f(),
                p3 = new Vector2f(), p4 = new Vector2f();
        matrices.transformPosition(x,           y + height, p1); // нижний-левый
        matrices.transformPosition(x + width, y + height, p2); // нижний-правый
        matrices.transformPosition(x + width, y,           p3); // верхний-правый
        matrices.transformPosition(x,           y,           p4); // верхний-левый

        // 5) UV-координаты (в пикселях или нормализованные, в зависимости от вашей текстуры)
        float u1 = 0f,    v1 = 0f,
                u2 = 1f,    v2 = 1f;
        int   light   = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int   overlay = OverlayTexture.DEFAULT_UV;
        float nz      = 1f;

        // 6) Заполняем вершины в том же стиле, как у вас
        buffer
                .vertex(p1.x, p1.y, 0.0F)
                .color(255, 255, 255, 255)
                .light(light)
                .normal(0f, 0f, nz)
                .texture(u1, v2);
        buffer
                .vertex(p2.x, p2.y, 0.0F)
                .color(255, 255, 255, 255)
                .light(light)
                .normal(0f, 0f, nz)
                .texture(u2, v2);
        buffer
                .vertex(p3.x, p3.y, 0.0F)
                .color(255, 255, 255, 255)
                .light(light)
                .normal(0f, 0f, nz)
                .texture(u2, v1);
        buffer
                .vertex(p4.x, p4.y, 0.0F)
                .color(255, 255, 255, 255)
                .light(light)
                .normal(0f, 0f, nz)
                .texture(u1, v1);

        // 7) Завершаем и отрисовываем через ваш RenderLayer
        layer.draw(buffer.end());
    }
}