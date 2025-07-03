package ru.l0sty.frogdisplays.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Utility class for raycasting in Minecraft.
 * This class provides a method to perform a raycast from the player's camera position in the direction they are looking.
 */
public class RCUtil {

    public static BlockHitResult rCBlock(double maxDistance) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null)
            return null;

        Vec3d start = client.player.getCameraPosVec(1.0f);
        Vec3d direction = client.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        BlockHitResult hit = client.world.raycast(new net.minecraft.world.RaycastContext(
            start,
            end,
            net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
            net.minecraft.world.RaycastContext.FluidHandling.NONE,
            client.player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit;
        }

        return null;
    }
}