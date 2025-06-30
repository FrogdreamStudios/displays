package ru.l0sty.frogdisplays.mixins;

import me.inotsleep.utils.logging.LoggingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.freedesktop.gstreamer.Gst;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.l0sty.frogdisplays.downloader.GStreamerDownloadListener;
import ru.l0sty.frogdisplays.downloader.GStreamerDownloaderMenu;
import ru.l0sty.frogdisplays.downloader.GStreamerErrorScreen;
import ru.l0sty.frogdisplays.util.Utils;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(MinecraftClient.class)
public abstract class GStreamInitMixin {
    @Shadow
    public abstract void setScreen(@Nullable Screen guiScreen);

    @Unique
    private static final AtomicBoolean recursionDetector = new AtomicBoolean(false);

    @Unique
    private static boolean downloaded = false;

    /// This mixin is used to initialize GStreamer libraries when the game starts.
    /// It checks if GStreamer libraries are downloaded and initialized
    /// If not, it redirects the screen to a downloader menu or an error screen
    @Inject(at = @At("HEAD"), method = "setScreen", cancellable = true)
    public void redirScreen(Screen guiScreen, CallbackInfo ci) {
        if (!downloaded) {
            boolean recursionValue = recursionDetector.get();
            recursionDetector.set(true);

            if (
                !(guiScreen instanceof GStreamerDownloaderMenu) &&
                !(guiScreen instanceof GStreamerErrorScreen)
            ) {
                if (GStreamerDownloadListener.INSTANCE.isDone() && !GStreamerDownloadListener.INSTANCE.isFailed()) {
                    downloaded = true;
                    Gst.init("MediaPlayer");
                }
                else if (!GStreamerDownloadListener.INSTANCE.isDone() && !GStreamerDownloadListener.INSTANCE.isFailed()) {
                    LoggingManager.warn("GStreamer has not finished loading, displaying loading screen");
                    setScreen(new GStreamerDownloaderMenu(guiScreen));
                    ci.cancel();
                }
                else if (GStreamerDownloadListener.INSTANCE.isFailed()) {
                    downloaded = true;
                    LoggingManager.error("GStreamer failed to initialize");
                    setScreen(new GStreamerErrorScreen(guiScreen, Utils.detectPlatform().equals("windows") ? "Frog Displays failed to download libraries": "Frogdisplays failed to initialize GStreamer. You need to download GStreamer libraries manually and place them in the ./libs/gstreamer directory"));
                }
            }

            recursionDetector.set(recursionValue);
        }
    }
}