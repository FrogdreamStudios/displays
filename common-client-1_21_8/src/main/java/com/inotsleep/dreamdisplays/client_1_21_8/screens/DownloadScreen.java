package com.inotsleep.dreamdisplays.client_1_21_8.screens;

import com.inotsleep.dreamdisplays.client.downloader.Downloader;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets.ProgressListWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class DownloadScreen extends Screen {
    ProgressListWidget list;
    Font font = Minecraft.getInstance().font;

    Screen parent;

    public DownloadScreen(Screen parent) {
        super(Component.literal("Dream Displays downloading libraries"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(list = new ProgressListWidget(
                minecraft,
                0,
                0,
                0,
                Minecraft.getInstance().font.lineHeight * 2 + 10,
                0
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (Downloader.getInstance() == null) return;

        Downloader downloader = Downloader.getInstance();

        int currentY = height / 4;
        int lineMargin = 2;

        // Screen title
        guiGraphics.drawCenteredString(font, title, width / 2, currentY, 0xffffffff);
        currentY += font.lineHeight;

        guiGraphics.drawCenteredString(font, Component.literal(String.format("%.2f%%", downloader.getProgress()*100)), (int) (width / 4d + (width / 2d) * downloader.getProgress()), currentY, 0xffffffff);

        currentY += font.lineHeight;

        // Total progress
        renderProgressBarCentered(guiGraphics, width / 2, currentY, width / 2, font.lineHeight, downloader.getProgress(), 0xffffffff, 1, 2, 0xff000000, 0xffffffff);
        currentY += font.lineHeight * 4;

        Downloader.Task currentTask = downloader.getTask();

        // Task title
        guiGraphics.drawCenteredString(font, Component.literal(currentTask.getName()), width/2, currentY, 0xffffffff);
        currentY += font.lineHeight * 2;

        // Task progress
        Downloader.ArtifactDownloadingProgress taskProgress = currentTask.getTotalProgress();
        guiGraphics.drawCenteredString(font, Component.literal(String.format(
                Locale.US,
                "%.2f%% (%.2f/%.2f MB)",
                taskProgress.progress()*100, (double) taskProgress.downloadedBytes() / 0x100000, (double) taskProgress.totalBytes() / 0x100000
        )), width/2, currentY, 0xffffffff);
        currentY += font.lineHeight + lineMargin;

        renderProgressBarCentered(guiGraphics, width / 2, currentY, width/2, font.lineHeight, taskProgress.progress(), 0xffffffff, 1, 2, 0xff000000, 0xffffffff);
        currentY += font.lineHeight * 4;

        list.setY(currentY);
        list.setX(width / 4);
        list.setWidth(width / 2);
        list.setHeight(Math.max(height - currentY - font.lineHeight * 5, 40));

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (downloader.isDone()) Minecraft.getInstance().setScreen(parent);
    }

    public static void renderProgressBarCentered(GuiGraphics guiGraphics, int x, int y, int width, int height, double progress, int borderColor, int borderWidth, int padding, int backgroundColor, int fillColor) {
        renderProgressBar(guiGraphics, x - width / 2, y, width, height, progress, borderColor, borderWidth, padding, backgroundColor, fillColor);
    }

    public static void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, double progress, int borderColor, int borderWidth, int padding, int backgroundColor, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + borderWidth, y + borderWidth, x + width - borderWidth, y + height - borderWidth, backgroundColor);
        guiGraphics.fill(x + padding, y + padding, (int) (x + (width - padding) * progress), y + height - padding, fillColor);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
