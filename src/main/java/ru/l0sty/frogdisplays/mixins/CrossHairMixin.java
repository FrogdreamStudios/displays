package ru.l0sty.frogdisplays.mixins;

import net.minecraft.client.render.RenderTickCounter;
import ru.l0sty.frogdisplays.PlatformlessInitializer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
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