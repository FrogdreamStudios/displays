package ru.l0sty.dreamdisplays.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.l0sty.dreamdisplays.screen.Screen;
import ru.l0sty.dreamdisplays.screen.ScreenManager;

public class ScreenWorldRenderer {

    /**
     * Renders all screens in the world based on their positions and facing directions.
     * @param matrices the matrix stack to use for rendering.
     * @param camera the camera we use to get the player's position and orientation.
     */
    public static void render(MatrixStack matrices, Camera camera) {
        Vec3d cameraPos = camera.getPos();
        for (Screen screen : ScreenManager.getScreens()) {
            if (screen == null) continue;
            if (screen.texture == null) screen.createTexture();

            matrices.push();

            // Translate the matrix stack to the player's screen position
            BlockPos pos = screen.getPos();
            Vec3d screenCenter = Vec3d.of(pos);
            Vec3d relativePos = screenCenter.subtract(cameraPos);
            matrices.translate(relativePos.x, relativePos.y, relativePos.z);

            // Move the matrix stack forward based on the screen's facing direction
            Tessellator tessellator = Tessellator.getInstance();

            renderScreenTexture(screen, matrices, tessellator);

            matrices.pop();
        }
    }

    /**
     * Renders the texture of a screen based on its facing direction and dimensions.
     * @param screen the screen to render.
     * @param matrices the matrix stack to use for rendering.
     * @param tessellator the tessellator to use for rendering.
     */
    private static void renderScreenTexture(Screen screen, MatrixStack matrices, Tessellator tessellator) {
        matrices.push();
        RenderUtil.moveForward(matrices, screen.getFacing(), 0.008f);

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

        // Fix the rotation of the matrix stack based on the screen's facing direction
        RenderUtil.fixRotation(matrices, screen.getFacing());
        matrices.scale(screen.getWidth(), screen.getHeight(), 0);

        // Render the screen texture or preview texture
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