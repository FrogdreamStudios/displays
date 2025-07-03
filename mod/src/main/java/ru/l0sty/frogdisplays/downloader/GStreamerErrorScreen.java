package ru.l0sty.frogdisplays.downloader;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

///  Error screen for GStreamer download issues.
/// This screen is displayed when there is an error during the GStreamer download process.
/// It allows the user to return to the previous screen by clicking the "Continue" button.
public class GStreamerErrorScreen extends Screen {
    private final Screen parent;
    private final String errorMessage;

    /// Constructor for GStreamerErrorScreen.
    /// @param parent the parent screen to return to when the user clicks "Continue".
    /// @param errorMessage the error message to display on the screen.
    public GStreamerErrorScreen(Screen parent, String errorMessage) {
        super(Text.of("Error while downloading GStreamer"));
        this.parent = parent;
        this.errorMessage = errorMessage;
    }

    ///  Initializes the GStreamer error screen.
    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(
                ButtonWidget.builder(Text.of("Continue"), button -> {
                            if (client != null) {
                                client.setScreen(parent);
                            }
                        })
                        .dimensions(this.width / 2 - 50, this.height / 2 + 40, 100, 20)
                        .build()
        );
    }

    ///  Renders the GStreamer error screen.
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        String titleText = this.title.getString();

        // Centered title
        int titleWidth = this.textRenderer.getWidth(titleText);

        // Draw the title text in the center of the screen
        context.drawText(textRenderer, titleText, (int) ((this.width - titleWidth) / 2f), (int) (this.height / 2f - 40f), 0xFF5555, true);

        // Error message
        int msgWidth = this.textRenderer.getWidth(errorMessage);
        context.drawText(textRenderer, errorMessage, (int) ((this.width - msgWidth) / 2f), (int) (this.height / 2f - 20f), 0xFF5555, true);

        super.render(context, mouseX, mouseY, delta);
    }

    /// Prevents the screen from closing when the Escape key is pressed.
    /// @return false to indicate that the screen should not close on Escape.
    @Override
    public boolean shouldCloseOnEsc() {

        return false;
    }
}
