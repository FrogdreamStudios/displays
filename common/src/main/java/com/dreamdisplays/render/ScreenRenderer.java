package com.dreamdisplays.render;

import com.dreamdisplays.screen.Manager;
import com.dreamdisplays.screen.Screen;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ScreenRenderer {

    // Renders all screens in the world relative to the camera position
    public static void render(PoseStack matrices, Camera camera) {
        Vec3 cameraPos = camera.position();
        for (Screen screen : Manager.getScreens()) {
            if (screen.texture == null) screen.createTexture();

            matrices.pushPose();

            // Translate the matrix stack to the player's screen position
            BlockPos pos = screen.getPos();
            Vec3 screenCenter = Vec3.atLowerCornerOf(pos);
            Vec3 relativePos = screenCenter.subtract(cameraPos);
            matrices.translate(relativePos.x, relativePos.y, relativePos.z);

            // Move the matrix stack forward based on the screen's facing direction
            Tesselator tessellator = Tesselator.getInstance();

            renderScreenTexture(screen, matrices, tessellator);

            matrices.popPose();
        }
    }

    // Renders the texture of a single screen
    private static void renderScreenTexture(
        Screen screen,
        PoseStack matrices,
        Tesselator tessellator
    ) {
        matrices.pushPose();
        moveForward(matrices, screen.getFacing(), 0.008f);

        switch (screen.getFacing()) {
            case "NORTH":
                moveHorizontal(matrices, "NORTH", -(screen.getWidth()));
                moveForward(matrices, "NORTH", 1);
                break;
            case "SOUTH":
                moveHorizontal(matrices, "SOUTH", 1);
                moveForward(matrices, "SOUTH", 1);
                break;
            case "EAST":
                moveHorizontal(matrices, "EAST", -(screen.getWidth() - 1));
                moveForward(matrices, "EAST", 2);
                break;
        }

        // Fix the rotation of the matrix stack based on the screen's facing direction
        fixRotation(matrices, screen.getFacing());
        matrices.scale(screen.getWidth(), screen.getHeight(), 0);

        // Render the screen texture or black square
        if (
            screen.isVideoStarted() &&
            screen.texture != null &&
            screen.renderType != null
        ) {
            renderGpuTexture(matrices, tessellator, screen.renderType);
        } else if (screen.renderType != null) {
            renderColor(matrices, tessellator, screen.renderType);
        }
        matrices.popPose();
    }

    // Prevent rotation issues
    private static void fixRotation(PoseStack stack, String facing) {
        final Quaternionf rotation;

        switch (facing) {
            case "NORTH":
                rotation = new Quaternionf().rotationY(
                    (float) Math.toRadians(180)
                );
                stack.translate(0, 0, 1);
                break;
            case "WEST":
                rotation = new Quaternionf().rotationY(
                    (float) Math.toRadians(-90.0)
                );
                stack.translate(0, 0, 0);
                break;
            case "EAST":
                rotation = new Quaternionf().rotationY(
                    (float) Math.toRadians(90.0)
                );
                stack.translate(-1, 0, 1);
                break;
            default:
                rotation = new Quaternionf();
                stack.translate(-1, 0, 0);
                break;
        }
        stack.mulPose(rotation);
    }

    // Moves the matrix stack forward based on the facing direction
    private static void moveForward(
        PoseStack stack,
        String facing,
        float amount
    ) {
        switch (facing) {
            case "NORTH":
                stack.translate(0, 0, -amount);
                break;
            case "WEST":
                stack.translate(-amount, 0, 0);
                break;
            case "EAST":
                stack.translate(amount, 0, 0);
                break;
            default:
                stack.translate(0, 0, amount);
                break;
        }
    }

    // Moves the matrix stack horizontally based on the facing direction
    private static void moveHorizontal(
        PoseStack stack,
        String facing,
        float amount
    ) {
        switch (facing) {
            case "NORTH":
                stack.translate(-amount, 0, 0);
                break;
            case "WEST":
                stack.translate(0, 0, amount);
                break;
            case "EAST":
                stack.translate(0, 0, -amount);
                break;
            default:
                stack.translate(amount, 0, 0);
                break;
        }
    }

    // Renders a GPU texture onto a quad using the provided matrix stack and tessellator
    private static void renderGpuTexture(
        PoseStack matrices,
        Tesselator tess,
        RenderType type
    ) {
        Matrix4f mat = matrices.last().pose();

        BufferBuilder buf = tess.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.BLOCK
        );

        buf
            .addVertex(mat, 0f, 0f, 0f)
            .setColor(255, 255, 255, 255)
            .setUv(0f, 1f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        buf
            .addVertex(mat, 1f, 0f, 0f)
            .setColor(255, 255, 255, 255)
            .setUv(1f, 1f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        buf
            .addVertex(mat, 1f, 1f, 0f)
            .setColor(255, 255, 255, 255)
            .setUv(1f, 0f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        buf
            .addVertex(mat, 0f, 1f, 0f)
            .setColor(255, 255, 255, 255)
            .setUv(0f, 0f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        MeshData built = buf.buildOrThrow();
        type.draw(built);
    }

    // Renders a solid color square with the specified RGB values
    private static void renderColor(
        PoseStack matrices,
        Tesselator tess,
        RenderType type
    ) {
        Matrix4f mat = matrices.last().pose();

        BufferBuilder buf = tess.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.BLOCK
        );

        buf
            .addVertex(mat, 0f, 0f, 0f)
            .setColor(0, 0, 0, 255)
            .setUv(0f, 1f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        buf
            .addVertex(mat, 1f, 0f, 0f)
            .setColor(0, 0, 0, 255)
            .setUv(1f, 1f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        buf
            .addVertex(mat, 1f, 1f, 0f)
            .setColor(0, 0, 0, 255)
            .setUv(1f, 0f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        buf
            .addVertex(mat, 0f, 1f, 0f)
            .setColor(0, 0, 0, 255)
            .setUv(0f, 0f)
            .setLight(0xF000F0)
            .setNormal(0f, 0f, 1f);

        MeshData built = buf.buildOrThrow();
        type.draw(built);
    }
}
