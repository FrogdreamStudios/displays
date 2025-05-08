package ru.l0sty.frogdisplays.downloader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Unique;

public class GStreamerDownloadListener {
    // TODO: I kinda would like to keep other mods from accessing this, but mixin complicates stuff
    @Unique
    public static final GStreamerDownloadListener INSTANCE = new GStreamerDownloadListener();

    private String task;
    private float percent;
    private boolean done;
    private boolean failed;

    public void setTask(String name) {
        this.task = name;
        this.percent = 0;
    }

    public String getTask() {
        return task;
    }

    public void setProgress(float percent) {
        this.percent =  ((float) (((int) (percent * 100))%100))/100;
    }

    public float getProgress() {
        return percent;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public boolean isDone() {
        return done;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof GStreamerDownloaderMenu menu) {
            screen = menu.menu;
        }

        MinecraftClient.getInstance().setScreen(new GStreamerErrorScreen(screen, "Не удалось инициализировать библиотеки. Обратитесь к разработчику мода"));
    }

    public boolean isFailed() {
        return failed;
    }
}