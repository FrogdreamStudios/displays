package ru.l0sty.frogdisplays.mixins;

import me.inotsleep.utils.LoggerFactory;
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

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(MinecraftClient.class)
public abstract class GStreamInitMixin {
    @Shadow
    public abstract void setScreen(@Nullable Screen guiScreen);

    @Unique
    private static final AtomicBoolean recursionDetector = new AtomicBoolean(false);

    @Unique
    private static boolean downloaded = false;

    @Inject(at = @At("HEAD"), method = "setScreen", cancellable = true)
    public void redirScreen(Screen guiScreen, CallbackInfo ci) {
        if (downloaded) {
            boolean recursionValue = recursionDetector.get();
            recursionDetector.set(true);

            if (
                    !recursionValue ||
                            !(guiScreen instanceof GStreamerDownloaderMenu)
            ) {
                if (GStreamerDownloadListener.INSTANCE.isDone() && !GStreamerDownloadListener.INSTANCE.isFailed()) {
                    downloaded = true;
                    Gst.init("MediaPlayer");
                }
                else if (!GStreamerDownloadListener.INSTANCE.isDone() && !GStreamerDownloadListener.INSTANCE.isFailed()) {
                    LoggerFactory.getLogger().warning("GStreamer has not finished loading, displaying loading screen.");
                    setScreen(new GStreamerDownloaderMenu(guiScreen));
                    ci.cancel();
                }
                else if (GStreamerDownloadListener.INSTANCE.isFailed()) {
                    LoggerFactory.getLogger().severe("MCEF failed to initialize!");
                }
            }

            recursionDetector.set(recursionValue);
        }
    }
}