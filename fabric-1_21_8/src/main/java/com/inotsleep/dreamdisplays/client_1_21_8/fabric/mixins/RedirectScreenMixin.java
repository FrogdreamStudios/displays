package com.inotsleep.dreamdisplays.client_1_21_8.fabric.mixins;

import com.inotsleep.dreamdisplays.client.downloader.Downloader;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.DownloadScreen;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.DownloadingErrorScreen;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class RedirectScreenMixin {
    @Shadow
    public abstract void setScreen(Screen guiScreen);

    @Inject(at = @At("HEAD"), method = "setScreen", cancellable = true)
    public void redirectScreen(Screen guiScreen, CallbackInfo callbackInfo) {
        LoggingManager.info("SET SCREEN CALL");

        Downloader downloader = Downloader.getInstance();

        if (downloader == null) {
            LoggingManager.info("DOWNLOADER IS NULL");
            return;
        }

        if (
                    guiScreen instanceof DownloadScreen ||
                    guiScreen instanceof DownloadingErrorScreen
        ) {
            LoggingManager.info("DOWNLOADING SCREEN SETTED");
            LoggingManager.info(guiScreen.toString());
            return;
        }

        if (downloader.isFailed()) {
            setScreen(new DownloadingErrorScreen());
            return;
        }
        if (downloader.isDone()) return;

        setScreen(new DownloadScreen(guiScreen));
        callbackInfo.cancel();
    }
}
