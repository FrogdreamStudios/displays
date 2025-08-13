package com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets;

import com.inotsleep.dreamdisplays.client.downloader.Downloader;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.DownloadScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class ProgressListWidget extends AbstractSelectionList<ProgressListWidget.ProgressEntry> {


    public ProgressListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, int headerHeight) {
        super(minecraft, width, height, y, itemHeight, headerHeight);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        clearEntries();

        Downloader
                .getInstance()
                .getTask()
                .getProgress()
                .stream()
                .filter((progress -> progress.progress() < 1))
                .map(
                        (progress) ->
                                new ProgressEntry(
                                        progress.name(),
                                        progress.progress(),
                                        progress.downloadedBytes(),
                                        progress.totalBytes()
                                )
                )
                .forEach(this::addEntry);

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public static class ProgressEntry extends Entry<ProgressEntry> {
        Font font = Minecraft.getInstance().font;

        private String name;
        private double progress;
        private long downloadedBytes;
        private long totalBytes;

        public ProgressEntry(String name, double progress, long downloadedBytes, long totalBytes) {
            this.totalBytes = totalBytes;
            this.downloadedBytes = downloadedBytes;
            this.progress = progress;
            this.name = name;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.fill(left+2, top+1, left + width-2, top + height-1, 0xf0ffffff);
            guiGraphics.fill(left+3, top+2, left + width-3, top + height-2, 0xf0000000);

            Component artifactName = Component.literal(trimTo(name, 20));
            guiGraphics.drawString(font, artifactName, left + 4, top+3, 0xffffffff);

            int progressX = Math.max(left + 4 + font.width(artifactName) + 4,  left + 8 + (width - 4) / 2);
            int progressWidth = width - 8 - progressX + left;

            DownloadScreen.renderProgressBar(guiGraphics, progressX, top+3, progressWidth, font.lineHeight, progress, 0xffffffff, 1, 2, 0xff000000, 0xffffffff);

            Component progressText = Component.literal(String.format(
                    Locale.US,
                    "%.2f%% (%.2f/%.2f MB)",
                    progress*100, (double) downloadedBytes / 0x100000, (double) totalBytes / 0x100000
            ));

            int textWidth = font.width(progressText);

            guiGraphics.drawString(font, progressText, left + width - 4 - textWidth, top+5+font.lineHeight, 0xffffffff);
        }

        public static String trimTo(String input, int len) {
            if (input == null || input.length() <= len) {
                return input;
            }

            return input.substring(0, len-3) + "...";
        }
    }
}
