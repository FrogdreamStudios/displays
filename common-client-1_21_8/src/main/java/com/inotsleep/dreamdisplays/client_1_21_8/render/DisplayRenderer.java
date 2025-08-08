package com.inotsleep.dreamdisplays.client_1_21_8.render;

import com.inotsleep.dreamdisplays.client.DisplayManager;
import com.inotsleep.dreamdisplays.client.display.Display;
import com.inotsleep.dreamdisplays.client.display.DisplayRenderData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class DisplayRenderer {
    public static void onRenderCall(PoseStack stack) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        stack.pushPose();
        Vec3 cameraPos = camera.getPosition();
        stack.translate(cameraPos.x, cameraPos.y, cameraPos.z);

        for (Display display : DisplayManager.getDisplays()) {
            stack.pushPose();
            DisplayRenderData renderData = display.getRenderData();
            if (renderData == null) continue;

            stack.translate(-renderData.x(), -renderData.y(), -renderData.z());

            stack.scale(renderData.width(), renderData.height(), 0);

            TextureObject object = TextureObject.validateObject(DisplayManager.getData(TextureObject.class, display.getId()), renderData.textureWidth(), renderData.textureHeight(), display.getId());
            object.writeToTexture(renderData.image());
            object.render(stack);

            stack.popPose();
        }

        stack.popPose();
    }


}
