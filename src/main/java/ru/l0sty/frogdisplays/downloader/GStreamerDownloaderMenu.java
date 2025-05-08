package ru.l0sty.frogdisplays.downloader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class GStreamerDownloaderMenu extends Screen {
    public final Screen menu;

    public GStreamerDownloaderMenu(Screen menu) {
        super(Text.of("Frogdisplays устанавливает необходимые библиотеки..."));
        this.menu = menu;
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        double cx = width / 2d;
        double cy = height / 2d;

        double progressBarHeight = 14;
        double progressBarWidth = width / 3d; // TODO: base off screen with (1/3 of screen)

        MatrixStack poseStack = graphics.getMatrices();

        /* Draw Progress Bar */
        poseStack.push();
        poseStack.translate(cx, cy, 0);
        poseStack.translate(-progressBarWidth / 2d, -progressBarHeight / 2d, 0);
        graphics.fill(
                0, 0,
                (int) progressBarWidth,
                (int) progressBarHeight,
                -1
        );
        graphics.fill(
                2, 2,
                (int) progressBarWidth - 2,
                (int) progressBarHeight - 2,
                -16777215
        );
        graphics.fill(
                4, 4,
                (int) ((progressBarWidth - 4) * GStreamerDownloadListener.INSTANCE.getProgress()),
                (int) progressBarHeight - 4,
                -1
        );
        poseStack.pop();

        // putting this here incase I want to re-add a third line later on
        // allows me to generalize the code to not care about line count
        String[] text = new String[]{
                GStreamerDownloadListener.INSTANCE.getTask(),
                (Math.round(GStreamerDownloadListener.INSTANCE.getProgress() * 100)%100) + "%",
        };

        int oSet = ((textRenderer.fontHeight / 2) + ((textRenderer.fontHeight + 2) * (text.length + 2))) + 4;
        poseStack.push();
        poseStack.translate(
                (int) (cx),
                (int) (cy - oSet),
                0
        );
        // draw menu name
        graphics.drawText(
                textRenderer,
                title.getString(),
                (int) -(textRenderer.getWidth(title.getString()) / 2d), 0,
                0xFFFFFF,
                true
        );
        // draw text
        int index = 0;
        for (String s : text) {
            if (index == 1) {
                poseStack.translate(0, textRenderer.fontHeight + 2, 0);
            }

            poseStack.translate(0, textRenderer.fontHeight + 2, 0);
            graphics.drawText(
                    textRenderer,
                    s,
                    (int) -(textRenderer.getWidth(s) / 2d), 0,
                    0xFFFFFF,
                    true
            );
            index++;
        }
        poseStack.pop();
    }

    @Override
    public void tick() {
        if (GStreamerDownloadListener.INSTANCE.isDone() || GStreamerDownloadListener.INSTANCE.isFailed()) {
            MinecraftClient.getInstance().setScreen(menu);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}