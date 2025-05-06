package ru.l0sty.frogdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;      // если рендеришь GpuTexture
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.render.*;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static net.minecraft.client.render.RenderPhase.ENABLE_LIGHTMAP;

public final class RenderUtil {

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

    public static void moveVertical(MatrixStack matrixStack, float amount) {
        matrixStack.translate(0, amount, 0);
    }

    public static void renderGpuTexture(MatrixStack matrices, Tessellator tess, GpuTexture gpuTex, RenderLayer layer) {
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

    public static void renderBlack(MatrixStack matrixStack, Tessellator tessellator) {
        renderColor(matrixStack, tessellator, 0, 0, 0);
    }



}
