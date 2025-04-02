package ru.l0sty.frogdisplays.mixins;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFPlatform;
import com.cinemamod.mcef.MCEFSettings;
import com.llamalad7.mixinextras.sugar.Local;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.l0sty.frogdisplays.CinemaModClient.proxyAddress;

@Debug(export = true)
@Mixin(targets = {"com.cinemamod.mcef.CefUtil"})
public class CefUtilMixin {
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

//    @Inject(method = "init", at = @At(value = "INVOKE", target = "Ljava/nio/file/Path;toString()Ljava/lang/String;"), locals = LocalCapture.CAPTURE_FAILHARD, remap = false, cancellable = true)
//    private static void otherInit(CallbackInfoReturnable<Boolean> cir, @Local MCEFPlatform platform, @Local String[] cefSwitches) {
//        ArrayList<String> switches = new ArrayList<>(List.of(cefSwitches));
//        //switches.add("--proxy-server=127.0.0.1:2080");
//        cefSwitches = switches.toArray(new String[0]);
//        if (!CefApp.startup(cefSwitches)) {
//            cir.setReturnValue(false);
//        } else {
//            MCEFSettings settings = MCEF.getSettings();
//            CefSettings cefSettings = new CefSettings();
//            cefSettings.windowless_rendering_enabled = true;
//            if (settings.isUsingCache()) {
//                cefSettings.cache_path = getCachePath().toString();
//            }
//
//            Objects.requireNonNull(cefSettings);
//            cefSettings.background_color = cefSettings.new ColorType(0, 255, 255, 255);
//            if (!Objects.equals(settings.getUserAgent(), "null")) {
//                cefSettings.user_agent = settings.getUserAgent();
//            } else {
//                cefSettings.user_agent_product = "MCEF/2";
//            }
//            CefApp app = CefApp.getInstance(cefSwitches, cefSettings);
//            setCefAppInstance(app);
//            setCefClientInstance(app.createClient());
//            setInit(true);
//            cir.setReturnValue(true);
//        }
//    }

}
