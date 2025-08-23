package com.inotsleep.dreamdisplays.client_1_21_8.forge.mixins;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class OutlineMixin {
    @Inject(method="renderHitOutline", at = @At(value = "HEAD"), cancellable = true)
    public void renderHitOutline(PoseStack poseStack, VertexConsumer buffer, Entity entity, double camX, double camY, double camZ, BlockPos pos, BlockState state, int color, CallbackInfo ci) {
        if (
                DreamDisplaysClientCommon.isOnScreen
        ) {
            ci.cancel();
        }
    }
}
