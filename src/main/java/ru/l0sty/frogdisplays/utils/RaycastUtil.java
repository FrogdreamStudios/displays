package ru.l0sty.frogdisplays.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class RaycastUtil {

    public static BlockHitResult raycastBlockClient(double maxDistance) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null)
            return null;

        Vec3d start = client.player.getCameraPosVec(1.0f);
        Vec3d direction = client.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        HitResult hit = client.world.raycast(new net.minecraft.world.RaycastContext(
            start,
            end,
            net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
            net.minecraft.world.RaycastContext.FluidHandling.NONE,
            client.player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return (BlockHitResult) hit;
        }

        return null;
    }
}
