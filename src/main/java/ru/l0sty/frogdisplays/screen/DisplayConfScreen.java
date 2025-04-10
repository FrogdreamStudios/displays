package ru.l0sty.frogdisplays.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import ru.l0sty.frogdisplays.CinemaMod;
import ru.l0sty.frogdisplays.CinemaModClient;
import ru.l0sty.frogdisplays.render.RenderUtil2D;
import ru.l0sty.frogdisplays.screen.widgets.IconButtonWidget;
import ru.l0sty.frogdisplays.screen.widgets.ToggleWidget;

import java.util.List;

public class DisplayConfScreen extends Screen {
    SliderWidget volume = null;
    SliderWidget renderD = null;
    SliderWidget quality = null;
    ToggleWidget poor = null;
    ToggleWidget sync = null;

    IconButtonWidget backButton = null;
    IconButtonWidget forwardButton = null;
    IconButtonWidget pauseButton = null;

    IconButtonWidget renderDReset = null;
    IconButtonWidget qualityReset = null;
    IconButtonWidget poorReset = null;
    IconButtonWidget syncReset = null;

    ru.l0sty.frogdisplays.screen.Screen screen;

    private float textScale;

    protected DisplayConfScreen() {
        super(Text.of("FrogDisplays"));
    }

    @Override
    protected void init() {
        volume = new SliderWidget(0, 0, 0, 0, Text.literal((int) Math.floor(screen.getVolume() * 100) + "%"), screen.getVolume()) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal((int) Math.floor(value * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                screen.setVolume((float) value);
            }
        };

        backButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(CinemaMod.MODID, "textures/gui/bbi.png"), 2) {
            @Override
            public void onPress() {
                screen.seekBackward();
            }
        };

        forwardButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(CinemaMod.MODID, "textures/gui/bfi.png"), 2) {
            @Override
            public void onPress() {
                screen.seekForward();
            }
        };

        pauseButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(CinemaMod.MODID, "textures/gui/bpi.png"), 2) {
            @Override
            public void onPress() {
                screen.setPaused(!screen.getPaused());
                setIconTexture(screen.getPaused() ? Identifier.of(CinemaMod.MODID, "textures/gui/bupi.png") : Identifier.of(CinemaMod.MODID, "textures/gui/bpi.png"));
            }
        };

        renderD = new SliderWidget(0, 0, 0, 0, Text.of(String.valueOf((int) CinemaModClient.maxDistance)), (CinemaModClient.maxDistance-24)/(96-24)) {
            @Override
            protected void updateMessage() {
                setMessage(Text.of(String.valueOf((int) (value*(96-24)) + 24)));
            }

            @Override
            protected void applyValue() {
                CinemaModClient.maxDistance = value * (96-24) + 24;
            }
        };

        int lq = 4;
        quality = new SliderWidget(0, 0, 0, 0, Text.of(screen.getQuality()+"p"), (double) fromQuality(screen.getQuality() + "p") /7) {
            @Override
            protected void updateMessage() {
                setMessage(Text.of(toQuality((int) (value*7))));
            }

            @Override
            protected void applyValue() {
                if (lq != (int) (value*7)) {
                    screen.setQuality(toQuality((int) (value * 7)).replace("p", ""));
                }
            }
        };

        renderDReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(CinemaMod.MODID, "textures/gui/bri.png"), 2) {
            @Override
            public void onPress() {
                CinemaModClient.maxDistance = 64;
            }
        };

        qualityReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(CinemaMod.MODID, "textures/gui/bri.png"), 2) {
            @Override
            public void onPress() {
                screen.setQuality(toQuality(3).replace("p", ""));
            }
        };

        addDrawableChild(volume);
        addDrawableChild(backButton);
        addDrawableChild(forwardButton);
        addDrawableChild(pauseButton);
        addDrawableChild(renderD);
        addDrawableChild(quality);
        addDrawableChild(qualityReset);
        addDrawableChild(renderDReset);
    }


    // Метод для отрисовки tooltip, если курсор находится над элементом
    private void renderTooltipIfHovered(DrawContext context, int mouseX, int mouseY,
                                        int elementX, int elementY, int elementWidth, int elementHeight,
                                        List<Text> tooltip) {
        if (mouseX >= elementX && mouseX <= elementX + elementWidth &&
                mouseY >= elementY && mouseY <= elementY + elementHeight) {
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рисуем фон
        renderBackground(context, mouseX, mouseY, delta);

        // Заголовок
        Text headerText = Text.literal("FrogDisplays");
        int headerTextWidth = textRenderer.getWidth(headerText);
        int headerTextX = (this.width - headerTextWidth) / 2;
        int headerTextY = 15;
        context.drawText(textRenderer, headerText, headerTextX, headerTextY, 0xFFFFFF, true);

        int maxSW = this.width / 3;

        // Расчёт размеров экрана
        int sW = maxSW;
        int sH = (int) Math.min((int) (screen.getHeight() / screen.getWidth() * sW), this.height / 3.5);
        sW = (int) (screen.getWidth() / screen.getHeight() * sH);
        int sX = this.width / 2 - sW / 2;
        int cY = textRenderer.fontHeight + 15 * 2;

        // Рисуем прямоугольник и экран
        //context.fill(this.width / 2 - maxSW / 2, cY, this.width / 2 + maxSW / 2, cY + sH, 0xff000000);
        renderScreen(context, sX, cY, sW, sH);

        cY += sH;
        int vCH = 25;
        cY += 5;

        // Настройка элементов управления (volume, backButton, forwardButton, pauseButton)
        volume.setX(this.width / 2 - maxSW / 2);
        volume.setY(cY);
        volume.setHeight(vCH);
        volume.setWidth(Math.min(maxSW / 3, maxSW / 2 - vCH * 9 / 8 - 5));

        backButton.setX(this.width / 2 - vCH * 9 / 8);
        backButton.setY(cY);
        backButton.setHeight(vCH);
        backButton.setWidth(vCH);

        forwardButton.setX(this.width / 2 + vCH / 8);
        forwardButton.setY(cY);
        forwardButton.setHeight(vCH);
        forwardButton.setWidth(vCH);

        pauseButton.setX(this.width / 2 + maxSW / 2 - vCH);
        pauseButton.setY(cY);
        pauseButton.setHeight(vCH);
        pauseButton.setWidth(vCH);

        cY += 10 + vCH;

        // Настройка кнопок renderD и renderDReset
        renderD.setX(this.width / 2 + maxSW / 2 - 80 - vCH - 5);
        renderD.setY(cY);
        renderD.setHeight(vCH);
        renderD.setWidth(80);

        renderDReset.setX(this.width / 2 + maxSW / 2 - vCH);
        renderDReset.setY(cY);
        renderDReset.setHeight(vCH);
        renderDReset.setWidth(vCH);

        // Рисуем текст кнопки "Прорисовка" и вычисляем координаты для tooltip
        Text renderDText = Text.literal("Прорисовка");
        int renderDTextX = this.width / 2 - maxSW / 2;
        int renderDTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, renderDText, renderDTextX, renderDTextY, 0xFFFFFF, true);

        // Tooltip для "Прорисовка"
        List<Text> renderDTooltip = List.of(
                Text.literal("Прорисовка").styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.literal("Определяет прорисовку, при которой").styled(style -> style.withColor(Formatting.GRAY)),
                Text.literal("активируются этот дисплей").styled(style -> style.withColor(Formatting.GRAY)),
                Text.empty(),
                Text.literal("Чтобы полностью отключить все").styled(style -> style.withColor(Formatting.DARK_GRAY)),
                Text.literal("дисплеи, пропиши /display off").styled(style -> style.withColor(Formatting.DARK_GRAY)),
                Text.empty(),
                Text.literal("Сейчас: " + (int) CinemaModClient.maxDistance + " блоков").styled(style -> style.withColor(Formatting.YELLOW))
        );
        renderTooltipIfHovered(context, mouseX, mouseY, renderDTextX, renderDTextY,
                textRenderer.getWidth(renderDText), textRenderer.fontHeight, renderDTooltip);

        cY += 5 + vCH;

        // Настройка кнопок quality и qualityReset
        quality.setX(this.width / 2 + maxSW / 2 - 80 - vCH - 5);
        quality.setY(cY);
        quality.setHeight(vCH);
        quality.setWidth(80);

        qualityReset.setX(this.width / 2 + maxSW / 2 - vCH);
        qualityReset.setY(cY);
        qualityReset.setHeight(vCH);
        qualityReset.setWidth(vCH);

        // Рисуем текст кнопки "Качество" и вычисляем координаты для tooltip
        Text qualityText = Text.literal("Качество");
        int qualityTextX = this.width / 2 - maxSW / 2;
        int qualityTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, qualityText, qualityTextX, qualityTextY, 0xFFFFFF, true);

        // Tooltip для "Качество"
        List<Text> qualityTooltip = List.of(
                Text.literal("Качество").styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.literal("Качество дисплея").styled(style -> style.withColor(Formatting.GRAY)),
                Text.empty(),
                Text.literal("Сейчас: " + screen.getQuality() + "p").styled(style -> style.withColor(Formatting.GOLD))
                //Text.literal(""),
                //Text.literal("С Premium можно смотреть").styled(style -> style.withColor(Formatting.DARK_GREEN)),
                //Text.literal("качественные дисплеи 1080p").styled(style -> style.withColor(Formatting.GREEN)))

        );
        renderTooltipIfHovered(context, mouseX, mouseY, qualityTextX, qualityTextY,
                textRenderer.getWidth(qualityText), textRenderer.fontHeight, qualityTooltip);

        cY += 5 + vCH;

        // Рендер дочерних элементов
        for (Element child : children()) {
            if (child instanceof Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }


    private void renderScreen(DrawContext context, int x, int y, int w, int h) {
        if (screen.isVideoStarted()) {
            int glId = screen.textureId;
            RenderUtil2D.drawTexturedQuad(context.getMatrices(), glId, x, y, w, h);
        } else if (screen.hasPreviewTexture()) {
            RenderUtil2D.drawTexturedQuad(context.getMatrices(), screen.getPreviewTexture().getGlId(), x, y, w, h);
        } else {
            context.fill(x, y, x + w, y + h, 0xFF000000);
        }
    }

    public static void open(ru.l0sty.frogdisplays.screen.Screen screen) {
        DisplayConfScreen displayConfScreen = new DisplayConfScreen();
        displayConfScreen.setScreen(screen);
        MinecraftClient.getInstance().setScreen(displayConfScreen);
    }

    private String toQuality(int resolution) {
        return switch (resolution) {
            case 0 -> "144p";
            case 1 -> "240p";
            case 2 -> "360p";
            case 4 -> "720p";
            case 5 -> "1080p";
            case 6 -> "1440p";
            case 7 -> "2160p";
            default -> "480p";
        };

    }

    private int fromQuality(String quality) {
        return switch (quality) {
            case "144p" -> 0;
            case "240p" -> 1;
            case "360p" -> 2;
            case "720p" -> 4;
            case "1080p" -> 5;
            case "1440p" -> 6;
            case "2160p" -> 7;
            default -> 3;
        };
    }

    private void setScreen(ru.l0sty.frogdisplays.screen.Screen screen) {
        this.screen = screen;
    }
}
