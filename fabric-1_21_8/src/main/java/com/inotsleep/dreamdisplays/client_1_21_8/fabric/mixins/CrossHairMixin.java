package com.inotsleep.dreamdisplays.client_1_21_8.fabric.mixins;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class CrossHairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (
                DreamDisplaysClientCommon.isOnScreen
        ) {
            ci.cancel();
        }
    }
}