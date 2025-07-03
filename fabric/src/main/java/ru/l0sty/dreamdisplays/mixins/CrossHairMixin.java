package ru.l0sty.dreamdisplays.mixins;

import net.minecraft.client.render.RenderTickCounter;
import ru.l0sty.dreamdisplays.PlatformlessInitializer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)

/**
 * Mixin to disable the crosshair rendering when Dream Displays is on screen.
 * This prevents the default crosshair from being displayed when Dream Displays is active.
 */
public class CrossHairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

        if (
                PlatformlessInitializer.isOnScreen
        ) {
            ci.cancel();
        }
    }
}