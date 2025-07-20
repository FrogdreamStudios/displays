package ru.l0sty.dreamdisplays.downloader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class GStreamerDownloaderMenu extends Screen {
    private final Screen parent;

    public GStreamerDownloaderMenu(Screen parent) {
        super(Text.literal("Dream Displays downloads GStreamer for display support"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.createNewRootLayer();

        int cx = width / 2;
        int cy = height / 2;
        int barW = width / 3;
        int barH = 14;
        int xPB = cx - barW / 2;
        int yPB = cy - barH / 2;

        context.fill(xPB, yPB, xPB + barW, yPB + barH, 0xFFFFFFFF);
        context.fill(xPB + 2, yPB + 2, xPB + barW - 2, yPB + barH - 2, 0xFF000000);
        float progress = GStreamerDownloadListener.INSTANCE.getProgress();
        int fillW = (int) ((barW - 4) * progress);
        context.fill(xPB + 2, yPB + 2, xPB + 2 + fillW, yPB + barH - 2, 0xFFFFFFFF);

        Text taskText    = Text.literal(GStreamerDownloadListener.INSTANCE.getTask());
        Text percentText = Text.literal(Math.round(progress * 100) + "%");
        int lineHeight = textRenderer.fontHeight + 2;

        context.drawCenteredTextWithShadow(textRenderer, this.title, cx, yPB - lineHeight * 2, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, taskText, cx, yPB - lineHeight, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, percentText, cx, yPB + barH + lineHeight / 2, 0xFFFFFFff);
    }

    @Override
    public void tick() {
        if (GStreamerDownloadListener.INSTANCE.isDone() || GStreamerDownloadListener.INSTANCE.isFailed()) {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
