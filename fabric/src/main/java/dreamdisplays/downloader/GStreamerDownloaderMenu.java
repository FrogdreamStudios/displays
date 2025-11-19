package dreamdisplays.downloader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

public class GStreamerDownloaderMenu extends Screen {
    public final Screen menu;

    /**
     * Menu for GStreamer download progress.
     */
    public GStreamerDownloaderMenu(Screen menu) {
        super(Component.nullToEmpty("Dream Displays downloads GStreamer for display support"));
        this.menu = menu;
    }

    /**
     * Render the background of the GStreamer download menu.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        float cx = (float) (width / 2d);
        float cy = (float) (height / 2d);

        float progressBarHeight = 14;
        float progressBarWidth = (float) (width / 3d);

        // TODO: base off screen with (1/3 of screen)

        Matrix3x2fStack matrix = graphics.pose();

        // Draw progress bar background
        matrix.pushMatrix();
        matrix.translate(cx, cy);
        matrix.translate((float) (-progressBarWidth / 2d), (float) (-progressBarHeight / 2d));
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
        matrix.popMatrix();

        String[] text = new String[]{
                GStreamerDownloadListener.INSTANCE.getTask(),
                (Math.round(GStreamerDownloadListener.INSTANCE.getProgress() * 100)%100) + "%",
        };

        int oSet = ((font.lineHeight / 2) + ((font.lineHeight + 2) * (text.length + 2))) + 4;
        matrix.pushMatrix();
        matrix.translate(
                (int) (cx),
                (int) (cy - oSet)
        );

        graphics.drawString(
                font,
                title.getString(),
                (int) -(font.width(title.getString()) / 2d), 0,
                0xFFFFFF,
                true
        );

        int index = 0;
        for (String s : text) {
            if (index == 1) {
                matrix.translate(0, font.lineHeight + 2);
            }

            matrix.translate(0, font.lineHeight + 2);
            graphics.drawString(
                    font,
                    s,
                    (int) -(font.width(s) / 2d), 0,
                    0xFFFFFF,
                    true
            );
            index++;
        }
        matrix.popMatrix();
    }

    /**
     * Tick the method to check if the download is done or failed.
     */
    @Override
    public void tick() {
        if (GStreamerDownloadListener.INSTANCE.isDone() || GStreamerDownloadListener.INSTANCE.isFailed()) {
            Minecraft.getInstance().setScreen(menu);
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