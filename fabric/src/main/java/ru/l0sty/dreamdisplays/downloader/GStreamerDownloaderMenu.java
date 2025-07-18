package ru.l0sty.dreamdisplays.downloader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

public class GStreamerDownloaderMenu extends Screen {
    public final Screen menu;

    /**
     * Menu for GStreamer download progress.
     */
    public GStreamerDownloaderMenu(Screen menu) {
        super(Text.of("Dream Displays downloads GStreamer for display support"));
        this.menu = menu;
    }

    /**
     * Render the background of the GStreamer download menu.
     */
    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        double cx = width / 2d;
        double cy = height / 2d;

        double progressBarHeight = 14;
        double progressBarWidth = width / 3d;

        // TODO: base off screen with (1/3 of screen)

        int progressBarX = (int) (cx - progressBarWidth / 2d);
        int progressBarY = (int) (cy - progressBarHeight / 2d);

        graphics.fill(
                progressBarX, progressBarY,
                (int) progressBarWidth + progressBarX,
                (int) progressBarHeight +progressBarY,
                -1
        );
        graphics.fill(
                progressBarX + 2, progressBarY + 2,
                (int) progressBarWidth + progressBarX - 2,
                (int) progressBarHeight + progressBarY - 2,
                -16777215
        );
        graphics.fill(
                4, 4,
                (int) ((progressBarWidth - 4) * GStreamerDownloadListener.INSTANCE.getProgress()),
                (int) progressBarHeight - 4,
                -1
        );

        String[] text = new String[]{
                GStreamerDownloadListener.INSTANCE.getTask(),
                (Math.round(GStreamerDownloadListener.INSTANCE.getProgress() * 100)%100) + "%",
        };

        int oSet = ((textRenderer.fontHeight / 2) + ((textRenderer.fontHeight + 2) * (text.length + 2))) + 4;

        int textX = (int) (cx -(textRenderer.getWidth(title.getString()) / 2d));
        int textY = (int) (cy - oSet);

        graphics.drawText(
                textRenderer,
                title.getString(),
                (int) textX, textY,
                0xFFFFFF,
                true
        );

        int index = 0;

        int yPosition = textY;
        for (String s : text) {
            if (index == 1) {
                yPosition+= textRenderer.fontHeight + 2;
            }

            yPosition+= textRenderer.fontHeight + 2;
            graphics.drawText(
                    textRenderer,
                    s,
                    (int) -(textRenderer.getWidth(s) / 2d), yPosition,
                    0xFFFFFF,
                    true
            );
            index++;
        }
    }

    /**
     * Tick the method to check if the download is done or failed.
     */
    @Override
    public void tick() {
        if (GStreamerDownloadListener.INSTANCE.isDone() || GStreamerDownloadListener.INSTANCE.isFailed()) {
            MinecraftClient.getInstance().setScreen(menu);
        }
    }

    /**
     * We determine if the screen should close on ESC key press.
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}