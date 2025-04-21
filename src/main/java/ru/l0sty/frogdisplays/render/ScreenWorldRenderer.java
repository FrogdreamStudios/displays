package ru.l0sty.frogdisplays.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.l0sty.frogdisplays.screen.Screen;
import ru.l0sty.frogdisplays.screen.ScreenManager;

public class ScreenWorldRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) {
                return;
            }
            MatrixStack matrices = context.matrixStack();
            Camera camera = context.camera();
            Vec3d cameraPos = camera.getPos();

            // Получаем список экранов, которые надо отрисовать.
            // Предполагается, что метод getScreens() возвращает коллекцию Screen.
            for (Screen screen : ScreenManager.getScreens()) {
                if (screen == null) continue;
                if (screen.removalTextureId != -1) GL11.glDeleteTextures(screen.removalTextureId);
                if (screen.textureId == -1) screen.createTexture();

                matrices.push();

                // Вычисляем позицию экрана относительно камеры
                BlockPos pos = screen.getPos();
                Vec3d screenCenter = Vec3d.of(pos);
                Vec3d relativePos = screenCenter.subtract(cameraPos);
                matrices.translate(relativePos.x, relativePos.y, relativePos.z);

                RenderSystem.enableDepthTest();
                Tessellator tessellator = Tessellator.getInstance();

                // Рендерим текстуру экрана с использованием уже реализованного метода
                renderScreenTexture(screen, matrices, tessellator);
                RenderSystem.disableDepthTest();

                matrices.pop();
            }
        });
    }

    private static void renderScreenTexture(Screen screen, MatrixStack matrices, Tessellator tessellator) {
        matrices.push();
        matrices.translate(1, 1, 0);
        RenderUtil.moveForward(matrices, screen.getFacing(), 1.008f);
        RenderUtil.moveVertical(matrices, screen.getHeight() - 1);

        // Применяем корректировку в зависимости от направления экрана
        switch (screen.getFacing()) {
            case "NORTH":
                RenderUtil.moveHorizontal(matrices, "NORTH", -(screen.getWidth() - 1));
                break;
            case "EAST":
                RenderUtil.moveHorizontal(matrices, "EAST", -(screen.getWidth() - 1));
                break;
        }

        RenderUtil.fixRotation(matrices, screen.getFacing());
        matrices.scale(screen.getWidth(), screen.getHeight(), 0);

        if (screen.isVideoStarted()) {
            screen.fitTexture();
            int glId = screen.textureId;
            RenderUtil.renderTexture(matrices, tessellator, glId);
        } else if (screen.hasPreviewTexture()) {
            NativeImageBackedTexture texture = screen.getPreviewTexture();
            RenderUtil.renderTexture(matrices, tessellator, texture.getGlId());
        } else {
            RenderUtil.renderBlack(matrices, tessellator);
        }
        matrices.pop();
    }
}
