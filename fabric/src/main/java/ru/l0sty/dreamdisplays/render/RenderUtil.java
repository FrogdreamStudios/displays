package ru.l0sty.dreamdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.render.*;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class RenderUtil {

    /**
     * Fixes the rotation of the matrix stack based on the facing direction.
     * @param matrixStack the matrix stack to modify.
     * @param facing the facing direction of the display (north, south, west, east).
     */
    public static void fixRotation(MatrixStack matrixStack, String facing) {
        final Quaternionf rotation;

        switch (facing) {
            case "NORTH":
                rotation = new Quaternionf().rotationY((float) Math.toRadians(180));
                matrixStack.translate(0, 0, 1);
                break;
            case "WEST":
                rotation = new Quaternionf().rotationY((float) Math.toRadians(-90.0));
                matrixStack.translate(0, 0, 0);
                break;
            case "EAST":
                rotation = new Quaternionf().rotationY((float) Math.toRadians(90.0));
                matrixStack.translate(-1, 0, 1);
                break;
            default:
                rotation = new Quaternionf();
                matrixStack.translate(-1, 0, 0);
                break;
        }
        matrixStack.multiply(rotation);
    }

    /**
     * Moves the matrix stack forward based on the facing direction.
     *
     * For example, if facing north, it will move the matrix stack forward by the specified amount in the negative Z direction.
     *
     * This is because of Minecraft's coordinate system.
     * @param matrixStack the matrix stack to modify.
     * @param facing the facing direction of the display (north, south, west, east).
     * @param amount the amount to move forward.
     */
    public static void moveForward(MatrixStack matrixStack, String facing, float amount) {
        switch (facing) {
            case "NORTH":
                matrixStack.translate(0, 0, -amount);
                break;
            case "WEST":
                matrixStack.translate(-amount, 0, 0);
                break;
            case "EAST":
                matrixStack.translate(amount, 0, 0);
                break;
            default:
                matrixStack.translate(0, 0, amount);
                break;
        }
    }

    /**
     * Moves the matrix stack horizontally based on the facing direction.
     */
    public static void moveHorizontal(MatrixStack matrixStack, String facing, float amount) {
        switch (facing) {
            case "NORTH":
                matrixStack.translate(-amount, 0, 0);
                break;
            case "WEST":
                matrixStack.translate(0, 0, amount);
                break;
            case "EAST":
                matrixStack.translate(0, 0, -amount);
                break;
            default:
                matrixStack.translate(amount, 0, 0);
                break;
        }
    }

    /**
     * Renders a GpuTexture using the specified matrices.
     * @param matrices the matrix stack to use for rendering.
     * @param tess the tessellator to use for rendering.
     * @param gpuTex the GpuTexture to render.
     * @param layer the RenderLayer to use for rendering.
     */
    public static void renderGpuTexture(MatrixStack matrices, Tessellator tess, GpuTextureView gpuTex, RenderLayer layer) {
        RenderSystem.setShaderTexture(0, gpuTex);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        BufferBuilder buf = tess.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL
        );

        buf
                .vertex(mat, 0f, 0f, 0f)
                .color(255, 255, 255, 255)
                .texture(0f, 1f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        buf
                .vertex(mat, 1f, 0f, 0f)
                .color(255, 255, 255, 255)
                .texture(1f, 1f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        buf
                .vertex(mat, 1f, 1f, 0f)
                .color(255, 255, 255, 255)
                .texture(1f, 0f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        buf
                .vertex(mat, 0f, 1f, 0f)
                .color(255, 255, 255, 255)
                .texture(0f, 0f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        BuiltBuffer built = buf.end();
        layer.draw(built);
    }

    /**
     * Just renders a solid color quad with the specified RGB values.
     */
    public static void renderColor(MatrixStack matrices, Tessellator tess, int r, int g, int b) {
        Matrix4f mat = matrices.peek().getPositionMatrix();

        BufferBuilder buf = tess.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL
        );

        buf
                .vertex(mat, 0f, 0f, 0f)
                .color(r, g, b, 255)
                .texture(0f, 1f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        buf
                .vertex(mat, 1f, 0f, 0f)
                .color(r, g, b, 255)
                .texture(1f, 1f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        buf
                .vertex(mat, 1f, 1f, 0f)
                .color(r, g, b, 255)
                .texture(1f, 0f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        buf
                .vertex(mat, 0f, 1f, 0f)
                .color(r, g, b, 255)
                .texture(0f, 0f)
                .light(0xF000F0)
                .normal(0f, 0f, 1f);

        BuiltBuffer built = buf.end();
        RenderLayer.getSolid().draw(built);
    }

    /**
     * Renders a solid black square.
     */
    public static void renderBlack(MatrixStack matrixStack, Tessellator tessellator) {
        renderColor(matrixStack, tessellator, 0, 0, 0);
    }
}