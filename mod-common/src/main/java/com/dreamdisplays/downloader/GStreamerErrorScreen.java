package com.dreamdisplays.downloader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GStreamerErrorScreen extends Screen {
    private final Screen parent;
    private final String errorMessage;

    // Constructor for GStreamerErrorScreen
    public GStreamerErrorScreen(Screen parent, String errorMessage) {
        super(Component.nullToEmpty("Error while downloading GStreamer"));
        this.parent = parent;
        this.errorMessage = errorMessage;
    }

    // Initializes the screen and adds the "Continue" button
    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(
                Button.builder(Component.nullToEmpty("Continue"), button -> {
                            minecraft.setScreen(parent);
                        })
                        .bounds(this.width / 2 - 50, this.height / 2 + 40, 100, 20)
                        .build()
        );
    }

    // Renders the error screen with title and error message
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        String titleText = this.title.getString();

        // Centered title
        int titleWidth = this.font.width(titleText);

        // Draw the title text in the center of the screen
        graphics.drawString(font, titleText, (int) ((this.width - titleWidth) / 2f), (int) (this.height / 2f - 40f), 0xFF5555, true);

        // Error message
        int msgWidth = this.font.width(errorMessage);
        graphics.drawString(font, errorMessage, (int) ((this.width - msgWidth) / 2f), (int) (this.height / 2f - 20f), 0xFF5555, true);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // Disable closing on ESC key
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
