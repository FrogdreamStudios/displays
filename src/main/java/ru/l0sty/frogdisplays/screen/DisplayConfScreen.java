package ru.l0sty.frogdisplays.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import ru.l0sty.frogdisplays.PlatformlessInitializer;
import ru.l0sty.frogdisplays.net.DeletePacket;
import ru.l0sty.frogdisplays.net.ReportPacket;
import ru.l0sty.frogdisplays.render.RenderUtil2D;
import ru.l0sty.frogdisplays.screen.widgets.IconButtonWidget;
import ru.l0sty.frogdisplays.screen.widgets.ToggleWidget;
import ru.l0sty.frogdisplays.screen.widgets.SliderWidget;

import java.util.List;
import java.util.Objects;

public class DisplayConfScreen extends Screen {
    SliderWidget volume = null;
    SliderWidget renderD = null;
    SliderWidget quality = null;
//    ToggleWidget focusMode = null;
    ToggleWidget sync = null;

    IconButtonWidget backButton = null;
    IconButtonWidget forwardButton = null;
    IconButtonWidget pauseButton = null;

    IconButtonWidget renderDReset = null;
    IconButtonWidget qualityReset = null;
//    IconButtonWidget focusModeReset = null;
    IconButtonWidget syncReset = null;

    IconButtonWidget deleteButton = null;
    IconButtonWidget reportButton = null;

    public ru.l0sty.frogdisplays.screen.Screen screen;

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

        backButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "bbi"), 2) {
            @Override
            public void onPress() {
                screen.seekBackward();
            }
        };

        forwardButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "bfi"), 2) {
            @Override
            public void onPress() {
                screen.seekForward();
            }
        };

        pauseButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "bpi"), 2) {
            @Override
            public void onPress() {
                screen.setPaused(!screen.getPaused());
                setIconTexture(screen.getPaused() ? Identifier.of(PlatformlessInitializer.MOD_ID, "bupi") : Identifier.of(PlatformlessInitializer.MOD_ID, "bpi"));
            }
        };

        pauseButton.setIconTexture(screen.getPaused() ? Identifier.of(PlatformlessInitializer.MOD_ID, "bupi") : Identifier.of(PlatformlessInitializer.MOD_ID, "bpi"));

        renderD = new SliderWidget(0, 0, 0, 0, Text.of(String.valueOf((int) PlatformlessInitializer.maxDistance)), (PlatformlessInitializer.maxDistance-24)/(96-24)) {
            @Override
            protected void updateMessage() {
                setMessage(Text.of(String.valueOf((int) (value*(96-24)) + 24)));
            }

            @Override
            protected void applyValue() {
                PlatformlessInitializer.maxDistance = value * (96-24) + 24;
            }
        };

        quality = new SliderWidget(0, 0, 0, 0, Text.of(screen.getQuality()+"p"), ((double) fromQuality(screen.getQuality())) / screen.getQualityList().size()) {
            @Override
            protected void updateMessage() {
                setMessage(Text.of(toQuality((int) (value*screen.getQualityList().size()))+"p"));
            }

            @Override
            protected void applyValue() {
                screen.setQuality(toQuality((int) (value * screen.getQualityList().size())));
            }
        };

        renderDReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "bri"), 2) {
            @Override
            public void onPress() {
                PlatformlessInitializer.maxDistance = 64;
                renderD.value = 64;
                renderD.setMessage(Text.of("64"));
            }
        };

        qualityReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "bri"), 2) {
            @Override
            public void onPress() {
                screen.setQuality(toQuality(2).replace("p", ""));
                quality.value = (double) 2 /screen.getQualityList().size();
                quality.setMessage(Text.of(toQuality(2)));
            }
        };

//        focusMode = new ToggleWidget(0, 0, 0, 0, Text.of(PlatformlessInitializer.focusMode ? "Вкл." : "Выкл."), PlatformlessInitializer.focusMode) {
//            @Override
//            protected void updateMessage() {
//                setMessage(Text.of(value ? "Вкл." : "Выкл."));
//            }
//
//            @Override
//            protected void applyValue() {
//                PlatformlessInitializer.focusMode = value;
//            }
//        };
//
//        focusModeReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "bri"), 2) {
//            @Override
//            public void onPress() {
//
//                PlatformlessInitializer.focusMode = false;
//            }
//        };

        sync = new ToggleWidget(0, 0, 0, 0, Text.of(screen.isSync ? "Вкл." : "Выкл."), screen.isSync) {
            @Override
            protected void updateMessage() {
                setMessage(Text.of(value ? "Вкл." : "Выкл."));
            }

            @Override
            protected void applyValue() {
                if (screen.owner) {
                    screen.isSync = value;
                    syncReset.active = value;
                    screen.waitForMFInit(() -> screen.sendSync());
                }
            }
        };

        syncReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "bri"), 2) {
            @Override
            public void onPress() {
                if (screen.owner) {
                    sync.value = false;
                    screen.waitForMFInit(() -> screen.sendSync());
                }
            }
        };

        sync.active = screen.owner;

        deleteButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "delete"), 2) {
            @Override
            public void onPress() {
                PlatformlessInitializer.sendPacket(new DeletePacket(screen.getID()));
                close();
            }
        };

        deleteButton.active = screen.owner;

        reportButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, Identifier.of(PlatformlessInitializer.MOD_ID, "report"), 2) {
            @Override
            public void onPress() {
                PlatformlessInitializer.sendPacket(new ReportPacket(screen.getID()));
                close();
            }
        };

        ButtonTextures textures = new ButtonTextures(Identifier.of(PlatformlessInitializer.MOD_ID, "widgets/red_button"), Identifier.of(PlatformlessInitializer.MOD_ID, "widgets/red_button_disabled"), Identifier.of(PlatformlessInitializer.MOD_ID, "widgets/red_button_highlighted"));

        deleteButton.setTextures(textures);
        reportButton.setTextures(textures);

        addDrawableChild(volume);
        addDrawableChild(backButton);
        addDrawableChild(forwardButton);
        addDrawableChild(pauseButton);
        addDrawableChild(renderD);
        addDrawableChild(quality);
        addDrawableChild(qualityReset);
        addDrawableChild(renderDReset);
        addDrawableChild(sync);
        addDrawableChild(syncReset);
        addDrawableChild(deleteButton);
        addDrawableChild(reportButton);
//        addDrawableChild(focusMode);
//        addDrawableChild(focusModeReset);
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
        Text headerText = Text.literal("FrogDisplays");

        int vCH = 25;

        deleteButton.setX(10);
        deleteButton.setY(this.height - vCH - 10);
        deleteButton.setHeight(vCH);
        deleteButton.setWidth(vCH);

        reportButton.setX(this.width - vCH - 10);
        reportButton.setY(this.height - vCH - 10);
        reportButton.setHeight(vCH);
        reportButton.setWidth(vCH);


        if (screen.errored) {
            volume.active = false;
            renderD.active = false;
            quality.active = false;
//            focusMode.active = false;
            sync.active = false;
            backButton.active = false;
            forwardButton.active = false;
            pauseButton.active = false;
            renderDReset.active = false;
            qualityReset.active = false;
//            focusModeReset.active = false;
            syncReset.active = false;

            List<Text> errorText = List.of(
                    Text.literal("Произошла ошибка!").styled(style -> style.withColor(0xff0000)),
                    Text.literal("Данное видно не возможно загрузить.").styled(style -> style.withColor(0xff0000)),
                    Text.literal("Попробуйте указать другое видео").styled(style -> style.withColor(0xff0000)),
                    Text.literal("").styled(style -> style.withColor(0xff0000)),
                    Text.literal("Обратите внимание! Видео с пометкой Youtube Детям ").styled(style -> style.withColor(0xff0000)),
                    Text.literal("воспроизводить невозможно из-за ограничений со стороны Youtube").styled(style -> style.withColor(0xff0000))
            );

            int yP = (int) ((double) this.height / 2 - ((double) (textRenderer.fontHeight + 2) * errorText.size()) / 2);

            int mW = 0;
            for (Text text : errorText) {
                mW = Math.max(textRenderer.getWidth(text), mW);
            }

            for (Text text : errorText) {
                context.drawText(textRenderer, text, this.width / 2 - textRenderer.getWidth(text) / 2, yP += 2 + textRenderer.fontHeight, 0xFFFFFF, true);
            }

            deleteButton.render(context, mouseX, mouseY, delta);
            reportButton.render(context, mouseX, mouseY, delta);

            return;
        }


        syncReset.active = screen.owner && screen.isSync;
//        focusModeReset.active = PlatformlessInitializer.focusMode;
        renderDReset.active = PlatformlessInitializer.maxDistance != 64;
        qualityReset.active = !Objects.equals(screen.getQuality(), "480");

        // Заголовок
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
        context.fill(this.width / 2 - maxSW / 2, cY, this.width / 2 + maxSW / 2, cY + sH, 0xff000000);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 10);
        renderScreen(context, sX, cY, sW, sH);
        context.getMatrices().pop();

        cY += sH;
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

        backButton.active = !(screen.isSync && !screen.owner);
        forwardButton.active = !(screen.isSync && !screen.owner);
        pauseButton.active = !(screen.isSync && !screen.owner);

        sync.active = (screen.owner);
        syncReset.active = (screen.owner);
        deleteButton.active = (screen.owner);

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
                Text.literal("Сейчас: " + (int) PlatformlessInitializer.maxDistance + " блоков").styled(style -> style.withColor(Formatting.YELLOW))
        );

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
                Text.literal("Сейчас: " + screen.getQuality() + "p").styled(style -> style.withColor(Formatting.GOLD)),
                Text.literal(""),
                Text.literal("С Premium можно смотреть").styled(style -> style.withColor(Formatting.DARK_GREEN)),
                Text.literal("качественные дисплеи 1080p").styled(style -> style.withColor(Formatting.GREEN))

        );

//        cY += 5 + vCH;

//        focusMode.setX(this.width / 2 + maxSW / 2 - 80 - vCH - 5);
//        focusMode.setY(cY);
//        focusMode.setHeight(vCH);
//        focusMode.setWidth(80);
//
//        focusModeReset.setX(this.width / 2 + maxSW / 2 - vCH);
//        focusModeReset.setY(cY);
//        focusModeReset.setHeight(vCH);
//        focusModeReset.setWidth(vCH);
//
//        // Рисуем текст кнопки "Качество" и вычисляем координаты для tooltip
//        Text focusModeText = Text.literal("Режим концентрации");
//        int focusModeTextX = this.width / 2 - maxSW / 2;
//        int focusModeTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
//        context.drawText(textRenderer, focusModeText, focusModeTextX, focusModeTextY, 0xFFFFFF, true);
//
//        List<Text> focusModeTooltip = List.of(
//                Text.literal("Режим концентрации").styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
//                Text.literal("Устанавливает, будет ли игрок получать эффект слепоты.").styled(style -> style.withColor(Formatting.GRAY)),
//                Text.empty(),
//                Text.literal("Сейчас: " + (PlatformlessInitializer.focusMode ? "вкл." : "выкл.")).styled(style -> style.withColor(Formatting.GOLD))
//        );


        cY += 15 + vCH;

        // Настройка кнопок quality и qualityReset
        sync.setX(this.width / 2 + maxSW / 2 - 80 - vCH - 5);
        sync.setY(cY);
        sync.setHeight(vCH);
        sync.setWidth(80);

        syncReset.setX(this.width / 2 + maxSW / 2 - vCH);
        syncReset.setY(cY);
        syncReset.setHeight(vCH);
        syncReset.setWidth(vCH);

        // Рисуем текст кнопки "Качество" и вычисляем координаты для tooltip
        Text syncText = Text.literal("Синхронизация");
        int syncTextX = this.width / 2 - maxSW / 2;
        int syncTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, syncText, syncTextX, syncTextY, 0xFFFFFF, true);

        List<Text> syncTooltip = List.of(
                Text.literal("Синхронизация").styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.literal("Опция доступна только для владельца дисплея.").styled(style -> style.withColor(Formatting.GRAY)),
                Text.literal("Устанавливает, будет ли дисплей синхронизироваться между игроками.").styled(style -> style.withColor(Formatting.GRAY)),
                Text.empty(),
                Text.literal("Сейчас: " + (sync.value ? "выкл." : "вкл.")).styled(style -> style.withColor(Formatting.GOLD))
        );

        renderTooltipIfHovered(context, mouseX, mouseY, renderDTextX, renderDTextY,
                textRenderer.getWidth(renderDText), textRenderer.fontHeight, renderDTooltip);
        renderTooltipIfHovered(context, mouseX, mouseY, qualityTextX, qualityTextY,
                textRenderer.getWidth(qualityText), textRenderer.fontHeight, qualityTooltip);
//        renderTooltipIfHovered(context, mouseX, mouseY, focusModeTextX, focusModeTextY,
//                textRenderer.getWidth(focusModeText), textRenderer.fontHeight, focusModeTooltip);
        renderTooltipIfHovered(context, mouseX, mouseY, syncTextX, syncTextY,
                textRenderer.getWidth(syncText), textRenderer.fontHeight, syncTooltip);

        // Рендер дочерних элементов
        for (Element child : children()) {
            if (child instanceof Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }


    private void renderScreen(DrawContext context, int x, int y, int w, int h) {
        if (screen.isVideoStarted()) {
            RenderUtil2D.drawTexturedQuad(context.getMatrices(), screen.texture.getGlTexture(), x, y, w, h, screen.renderLayer);
        } else if (screen.hasPreviewTexture()) {
            RenderUtil2D.drawTexturedQuad(context.getMatrices(), screen.getPreviewTexture().getGlTexture(), x, y, w, h, screen.previewRenderLayer);
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
        List<Integer> list = screen.getQualityList();

        if (list.isEmpty()) return "144p";


        int i = Math.max(Math.min(resolution, list.size() - 1), 0);
        return list.get(i).toString();
    }

    private int fromQuality(String quality) {
        List<Integer> list = screen.getQualityList();

        if (list.isEmpty()) return 0;
        int cQ = Integer.parseInt(quality.replace("p", ""));

        int res = list.stream().filter(q -> q==cQ).findAny().orElse(Math.max(Math.min(list.getLast(), cQ), list.getFirst()));
        return list.indexOf(list.contains(res) ? res: list.getFirst());
    }

    private void setScreen(ru.l0sty.frogdisplays.screen.Screen screen) {
        this.screen = screen;
    }
}
