package ru.l0sty.frogdisplays.mixins;

import org.cef.CefApp;
import org.cef.CefClient;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.Objects;

import static ru.l0sty.frogdisplays.CinemaModClient.proxyAddress;

@Debug(export = true)
@Mixin(targets = {"com.cinemamod.mcef.CefUtil"})
public abstract class CefUtilMixin {
    @Accessor(value = "cefAppInstance", remap = false)
    private static void setCefAppInstance(CefApp cefAppInstance) {
        throw new AssertionError();
    }

    @Accessor(value = "cefClientInstance", remap = false)
    private static void setCefClientInstance(CefClient cefClientInstance) {
        throw new AssertionError();
    }

    @Accessor(value = "init", remap = false)
    private static void setInit(boolean init) {
        throw new AssertionError();
    }
    @Accessor(value = "CACHE_PATH", remap = false)
    private static Path getCachePath() {
        throw new AssertionError();
    }

    @ModifyVariable(
            method = "Lcom/cinemamod/mcef/CefUtil;init()Z",
            at = @At(value = "STORE", ordinal = 0), // The first store of `cefSwitches`
            ordinal = 0,
            remap = false
    )
    private static String[] modifyCefSwitches(String[] original) {

        if (Objects.equals(proxyAddress, "")) return new String[]{"--autoplay-policy=no-user-gesture-required", "--disable-web-security", "--enable-widevine-cdm"};
        return new String[]{"--autoplay-policy=no-user-gesture-required", "--disable-web-security", "--enable-widevine-cdm", "--proxy-server="+proxyAddress};
    }

    @Inject(method = "Lcom/cinemamod/mcef/CefUtil;init()Z", at = @At("RETURN"), remap = false)
    private static void otherInit(CallbackInfoReturnable<Boolean> cir) {
    }

}
