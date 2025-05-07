package ru.l0sty.frogdisplays.downloader;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Экран ошибки для FrogDisplays, отображает сообщение об ошибке и кнопку "Продолжить".
 */
public class GStreamerErrorScreen extends Screen {
    private final Screen parent;
    private final String errorMessage;

    /**
     * @param parent экран, на который нужно вернуться при нажатии кнопки "Продолжить"
     * @param errorMessage текст сообщения об ошибке
     */
    public GStreamerErrorScreen(Screen parent, String errorMessage) {
        super(Text.of("Ошибка FrogDisplays"));
        this.parent = parent;
        this.errorMessage = errorMessage;
    }

    @Override
    protected void init() {
        super.init();
        // Кнопка "Продолжить"
        this.addDrawableChild(
                ButtonWidget.builder(Text.of("Продолжить"), button -> {
                            if (client != null) {
                                client.setScreen(parent);
                            }
                        })
                        .dimensions(this.width / 2 - 50, this.height / 2 + 40, 100, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Отрисовка фона
        this.renderBackground(context, mouseX, mouseY, delta);

        // Заголовок экрана
        String titleText = this.title.getString();
        int titleWidth = this.textRenderer.getWidth(titleText);
        context.drawText(textRenderer, titleText, (int) ((this.width - titleWidth) / 2f), (int) (this.height / 2f - 40f), 0xFF5555, true);

        // Сообщение об ошибке
        int msgWidth = this.textRenderer.getWidth(errorMessage);
        context.drawText(textRenderer, errorMessage, (int) ((this.width - msgWidth) / 2f), (int) (this.height / 2f - 20f), 0xFF5555, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Блокируем закрытие экрана через ESC
        return false;
    }
}
