package ru.l0sty.frogdisplays.mixins;

import net.minecraft.client.render.RenderTickCounter;
import ru.l0sty.frogdisplays.CinemaModClient;
import ru.l0sty.frogdisplays.cef.CefUtil;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

@Mixin(GameRenderer.class)
public class CefRenderMixin {
//
//    private static long RENDER_DELTA_NS = TimeUnit.MILLISECONDS.toNanos((long) Math.ceil(1000 / (float) 30));
//    private static long CHECK_SETTINGS_DELTA_NS = TimeUnit.MILLISECONDS.toNanos(1000);
//
//    private long lastRenderTime;
//    private long lastTickTime = 0;
//
//    private long lastCheckSettingsTime;
//    //     public void render(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
//    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
//    public void render(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
//        lastTickTime += (long) tickCounter.getLastDuration();
//        if ((lastTickTime - lastRenderTime) > RENDER_DELTA_NS) {
//            if (!CefUtil.isInit()) return;
//            CefUtil.getCefApp().N_DoMessageLoopWork();
//            lastRenderTime = lastTickTime;
//        }
//        if ((lastTickTime - lastCheckSettingsTime) > CHECK_SETTINGS_DELTA_NS) {
//            int refreshRate = CinemaModClient.getInstance().getVideoSettings().getBrowserRefreshRate();
//            RENDER_DELTA_NS = TimeUnit.MILLISECONDS.toNanos((long) Math.ceil(1000 / (float) refreshRate));
//            lastCheckSettingsTime = lastTickTime;
//        }
//    }

}