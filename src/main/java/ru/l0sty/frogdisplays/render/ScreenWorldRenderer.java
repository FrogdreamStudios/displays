package ru.l0sty.frogdisplays.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.l0sty.frogdisplays.screen.Screen;
import ru.l0sty.frogdisplays.screen.ScreenManager;

public class ScreenWorldRenderer {
    public static void render(MatrixStack matrices, Camera camera) {
        Vec3d cameraPos = camera.getPos();
        for (Screen screen : ScreenManager.getScreens()) {
            if (screen == null) continue;
            if (screen.texture == null) screen.createTexture();

            matrices.push();

            // Вычисляем позицию экрана относительно камеры
            BlockPos pos = screen.getPos();
            Vec3d screenCenter = Vec3d.of(pos);
            Vec3d relativePos = screenCenter.subtract(cameraPos);
            matrices.translate(relativePos.x, relativePos.y, relativePos.z);


            Tessellator tessellator = Tessellator.getInstance();

            // Рендерим текстуру экрана с использованием уже реализованного метода



            renderScreenTexture(screen, matrices, tessellator);

            matrices.pop();
        }
    }

    private static void renderScreenTexture(Screen screen, MatrixStack matrices, Tessellator tessellator) {
        matrices.push();
        RenderUtil.moveForward(matrices, screen.getFacing(), 0.008f);

        // Применяем корректировку в зависимости от направления экрана
        switch (screen.getFacing()) {
            case "NORTH":
                RenderUtil.moveHorizontal(matrices, "NORTH", -(screen.getWidth()));
                RenderUtil.moveForward(matrices, "NORTH", 1);
                break;
            case "SOUTH":
                RenderUtil.moveHorizontal(matrices, "SOUTH", 1);
                RenderUtil.moveForward(matrices, "SOUTH", 1);
                break;
            case "EAST":
                RenderUtil.moveHorizontal(matrices, "EAST", -(screen.getWidth() - 1));
                RenderUtil.moveForward(matrices, "EAST", 2);
                break;
        }

        RenderUtil.fixRotation(matrices, screen.getFacing());
        matrices.scale(screen.getWidth(), screen.getHeight(), 0);

//        if (screen.hasPreviewTexture()) {
//            RenderUtil.renderGpuTexture(matrices, tessellator, screen.getPreviewTexture().getGlTexture(), screen.previewTextureId);
//        }


        if (screen.isVideoStarted()) {
            screen.fitTexture();
            RenderUtil.renderGpuTexture(matrices, tessellator, screen.texture.getGlTexture(), screen.renderLayer);
        } else if (screen.hasPreviewTexture()) {
            RenderUtil.renderGpuTexture(matrices, tessellator, screen.getPreviewTexture().getGlTexture(), screen.previewRenderLayer);
        } else {
            RenderUtil.renderBlack(matrices, tessellator);
        }
        matrices.pop();
    }
}
