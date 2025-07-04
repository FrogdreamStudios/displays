package ru.l0sty.dreamdisplays.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import ru.l0sty.dreamdisplays.PlatformlessInitializer;
import ru.l0sty.dreamdisplays.net.DeletePacket;
import ru.l0sty.dreamdisplays.net.ReportPacket;
import ru.l0sty.dreamdisplays.render.RenderUtil2D;
import ru.l0sty.dreamdisplays.screen.widgets.IconButtonWidget;
import ru.l0sty.dreamdisplays.screen.widgets.ToggleWidget;
import ru.l0sty.dreamdisplays.screen.widgets.SliderWidget;

import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration screen for Dream Displays.
 * This screen allows users to configure various settings related to the Dream Displays mod, such as volume, rendering distance, quality, and synchronization options.
 * It provides a user interface for adjusting these settings and includes buttons for controlling playback, resetting values, and deleting or reporting displays.
 */
public class DisplayConfScreen extends Screen {

    // Language support
    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();

    static {
        Map<String, String> english = new HashMap<>();
        english.put("title", "Dream Displays");
        english.put("enabled", "Enabled");
        english.put("disabled", "Disabled");
        english.put("error_cant_load", "Oops! We can't load this video.");
        english.put("error_try_another", "Try to set another one.");
        english.put("error_no_kids", "Please do not enter YouTube Kids videos");
        english.put("error_limitations", "because it's not possible due to limitations from YouTube.");
        english.put("render_distance", "Render distance");
        english.put("render_distance_desc", "Determines the rendering distance at which");
        english.put("render_distance_desc2", "this display is activated");
        english.put("render_distance_disable", "To completely disable all displays, use");
        english.put("render_distance_command", "/display off");
        english.put("render_distance_current", "Current: %d blocks");
        english.put("quality", "Quality");
        english.put("quality_desc", "Quality of display");
        english.put("quality_current", "Now: %sp");
        english.put("synchronization", "Synchronization");
        english.put("sync_desc", "Option available only to the display owner");
        english.put("sync_desc2", "Sets whether the display will be synchronized between players");
        english.put("sync_current", "Now: %s");

        Map<String, String> russian = new HashMap<>();
        russian.put("title", "Dream Displays");
        russian.put("enabled", "Включено");
        russian.put("disabled", "Выключено");
        russian.put("error_cant_load", "Упс! Мы не можем загрузить это видео.");
        russian.put("error_try_another", "Попробуйте установить другое.");
        russian.put("error_no_kids", "Пожалуйста, не вводите видео YouTube Kids,");
        russian.put("error_limitations", "потому что это невозможно из-за ограничений YouTube.");
        russian.put("render_distance", "Рендеринг");
        russian.put("render_distance_desc", "Определяет расстояние рендеринга, на котором");
        russian.put("render_distance_desc2", "этот дисплей активируется");
        russian.put("render_distance_disable", "Чтобы полностью отключить все дисплеи, пропишите");
        russian.put("render_distance_command", "/display off");
        russian.put("render_distance_current", "Сейчас: %d блоков");
        russian.put("quality", "Качество");
        russian.put("quality_desc", "Качество отображения дисплея");
        russian.put("quality_current", "Сейчас: %sp");
        russian.put("synchronization", "Синхронизация");
        russian.put("sync_desc", "Опция доступна только владельцу дисплея");
        russian.put("sync_desc2", "Устанавливает, будет ли дисплей синхронизирован между игроками");
        russian.put("sync_current", "Сейчас: %s");

        Map<String, String> ukrainian = new HashMap<>();
        ukrainian.put("title", "Dream Displays");
        ukrainian.put("enabled", "Увімкнено");
        ukrainian.put("disabled", "Вимкнено");
        ukrainian.put("error_cant_load", "Упс! Ми не можемо завантажити це відео.");
        ukrainian.put("error_try_another", "Спробуйте встановити щось інше.");
        ukrainian.put("error_no_kids", "Будь ласка, не вводьте відео YouTube Kids,");
        ukrainian.put("error_limitations", "оскільки це неможливо через обмеження YouTube.");
        ukrainian.put("render_distance", "Дистанція рендеру");
        ukrainian.put("render_distance_desc", "Визначає дистанцію рендеру, на якій");
        ukrainian.put("render_distance_desc2", "цей дисплей активується");
        ukrainian.put("render_distance_disable", "Щоб повністю вимкнути всі дисплеї, пропишіть");
        ukrainian.put("render_distance_command", "/display off");
        ukrainian.put("render_distance_current", "Зараз: %d блоків");
        ukrainian.put("quality", "Якість");
        ukrainian.put("quality_desc", "Якість відображення дисплея");
        ukrainian.put("quality_current", "Зараз: %sp");
        ukrainian.put("synchronization", "Синхронізація");
        ukrainian.put("sync_desc", "Опція доступна лише власнику дисплея");
        ukrainian.put("sync_desc2", "Встановлює, чи буде дисплей синхронізовано між гравцями");
        ukrainian.put("sync_current", "Зараз: %s");

        Map<String, String> deutsch = new HashMap<>();
        deutsch.put("title", "Dream Displays");
        deutsch.put("enabled", "Aktiviert");
        deutsch.put("disabled", "Deaktiviert");
        deutsch.put("error_cant_load", "Ups! Wir können dieses Video nicht laden.");
        deutsch.put("error_try_another", "Versuchen Sie, etwas anderes einzustellen.");
        deutsch.put("error_no_kids", "Bitte geben Sie kein YouTube Kids Video ein,");
        deutsch.put("error_limitations", "da dies aufgrund von YouTube nicht möglich ist.");
        deutsch.put("render_distance", "Render-Distanz");
        deutsch.put("render_distance_desc", "Bestimmt die Render-Distanz, bei der");
        deutsch.put("render_distance_desc2", "dieses Anzeige aktiviert wird");
        deutsch.put("render_distance_disable", "Um alle Anzeigen vollständig zu deaktivieren, geben Sie ein");
        deutsch.put("render_distance_command", "/display off");
        deutsch.put("render_distance_current", "Aktuell: %d Blöcke");
        deutsch.put("quality", "Qualität");
        deutsch.put("quality_desc", "Display-Qualitätsverbesserungseinstellung");
        deutsch.put("quality_current", "Aktuell: %sp");
        deutsch.put("synchronization", "Synchronisierung");
        deutsch.put("sync_desc", "Option nur für den Besitzer der Anzeige verfügbar");
        deutsch.put("sync_desc2", "Legt fest, ob die Anzeige zwischen Spielern synchronisiert wird");
        deutsch.put("sync_current", "Aktuell: %s");

        Map<String, String> polish = new HashMap<>();
        polish.put("title", "Dream Displays");
        polish.put("enabled", "Aktywne");
        polish.put("disabled", "Nieaktywne");
        polish.put("error_cant_load", "Ups! Nie możemy załadować ten filmik.");
        polish.put("error_try_another", "Spróbuj ustawić coś innego.");
        polish.put("error_no_kids", "Proszę nie podawać filmu z YouTube Kids,");
        polish.put("error_limitations", "ponieważ nie jest to możliwe z powodu ograniczeń YouTube");
        polish.put("render_distance", "Renderowanie");
        polish.put("render_distance_desc", "Określa dystans renderowania, przy którym");
        polish.put("render_distance_desc2", "wyświetlacze są aktywowane");
        polish.put("render_distance_disable", "Aby całkowicie wyłączyć wszystkie wyświetlacze, wpisz");
        polish.put("render_distance_command", "/display off");
        polish.put("render_distance_current", "Obecnie: %d bloków");
        polish.put("quality", "Jakość");
        polish.put("quality_desc", "Ustawienie jakości wyświetlacza");
        polish.put("quality_current", "Obecnie: %sp");
        polish.put("synchronization", "Synchronizacja");
        polish.put("sync_desc", "Opcja dostępna tylko dla właściciela wyświetlacza");
        polish.put("sync_desc2", "Określa, czy wyświetlacz jest synchronizowany między graczami");
        polish.put("sync_current", "Obecnie: %s");

        TRANSLATIONS.put("en", english);
        TRANSLATIONS.put("ru", russian);
        TRANSLATIONS.put("ua", ukrainian);
        TRANSLATIONS.put("uk", ukrainian);
        TRANSLATIONS.put("de", deutsch);
        TRANSLATIONS.put("pl", polish);
    }

    private String getCurrentLanguage() {
        // Get language from Minecraft client settings
        String lang = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        // Extract base language code (e.g., "en" from "en_us")
        if (lang.contains("_")) {
            lang = lang.split("_")[0];
        }
        // Default to English if the language is not supported
        return TRANSLATIONS.containsKey(lang) ? lang : "en";
    }

    private String getTranslation(String key, Object... args) {
        String lang = getCurrentLanguage();
        String translation = TRANSLATIONS.get(lang).getOrDefault(key, TRANSLATIONS.get("en").get(key));
        return args.length > 0 ? String.format(translation, args) : translation;
    }

    SliderWidget volume = null;
    SliderWidget renderD = null;
    SliderWidget quality = null;
    ToggleWidget sync = null;

    IconButtonWidget backButton = null;
    IconButtonWidget forwardButton = null;
    IconButtonWidget pauseButton = null;

    IconButtonWidget renderDReset = null;
    IconButtonWidget qualityReset = null;
    IconButtonWidget syncReset = null;

    IconButtonWidget deleteButton = null;
    IconButtonWidget reportButton = null;

    public ru.l0sty.dreamdisplays.screen.Screen screen;

    protected DisplayConfScreen() {
        super(Text.of("Dream Displays"));
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
                screen.setQuality(toQuality(fromQuality("480")).replace("p", ""));
                quality.value = (double) 2 /screen.getQualityList().size();
                quality.setMessage(Text.of(toQuality(2) + "p"));
            }
        };

        sync = new ToggleWidget(0, 0, 0, 0, Text.of(screen.isSync ? getTranslation("enabled") : getTranslation("disabled")), screen.isSync) {
            @Override
            protected void updateMessage() {
                setMessage(Text.of(value ? getTranslation("enabled") : getTranslation("disabled")));
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
    }

    private void renderTooltipIfHovered(DrawContext context, int mouseX, int mouseY,
                                        int elementX, int elementY, int elementWidth, int elementHeight,
                                        List<Text> tooltip) {
        if (mouseX >= elementX && mouseX <= elementX + elementWidth &&
                mouseY >= elementY && mouseY <= elementY + elementHeight) {
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Renders the display configuration screen.
     * @param context the draw context for rendering.
     * @param mouseX the x-coordinate of the cursor.
     * @param mouseY the y-coordinate of the cursor.
     * @param delta the time delta since the last frame.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        renderBackground(context, mouseX, mouseY, delta);
        Text headerText = Text.literal(getTranslation("title"));

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
            sync.active = false;
            backButton.active = false;
            forwardButton.active = false;
            pauseButton.active = false;
            renderDReset.active = false;
            qualityReset.active = false;
            syncReset.active = false;

            List<Text> errorText = List.of(
                    Text.literal(getTranslation("error_cant_load")).styled(style -> style.withColor(0xff0000)),
                    Text.literal(getTranslation("error_try_another")).styled(style -> style.withColor(0xff0000)),
                    Text.literal("").styled(style -> style.withColor(0xff0000)),
                    Text.literal(getTranslation("error_no_kids")).styled(style -> style.withColor(0xff0000)),
                    Text.literal(getTranslation("error_limitations")).styled(style -> style.withColor(0xff0000))
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
        renderDReset.active = PlatformlessInitializer.maxDistance != 64;
        qualityReset.active = !Objects.equals(screen.getQuality(), "480");

        int headerTextWidth = textRenderer.getWidth(headerText);
        int headerTextX = (this.width - headerTextWidth) / 2;
        int headerTextY = 15;
        context.drawText(textRenderer, headerText, headerTextX, headerTextY, 0xFFFFFF, true);

        int maxSW = this.width / 3;

        // Screen dimensions
        int sW = maxSW;
        int sH = (int) Math.min((int) (screen.getHeight() / screen.getWidth() * sW), this.height / 3.5);
        sW = (int) (screen.getWidth() / screen.getHeight() * sH);
        int sX = this.width / 2 - sW / 2;
        int cY = textRenderer.fontHeight + 15 * 2;

        context.fill(this.width / 2 - maxSW / 2, cY, this.width / 2 + maxSW / 2, cY + sH, 0xff000000);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 10);
        renderScreen(context, sX, cY, sW, sH);
        context.getMatrices().pop();

        cY += sH;
        cY += 5;

        // Settings for volume, backButton, forwardButton, pauseButton
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
        deleteButton.active = (screen.owner);

        cY += 10 + vCH;

        // Volume, backButton, forwardButton, pauseButton
        placeButton(vCH, maxSW, cY, renderD, renderDReset);

        // Tooltip for Render Distance
        Text renderDText = Text.literal(getTranslation("render_distance"));
        int renderDTextX = this.width / 2 - maxSW / 2;
        int renderDTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, renderDText, renderDTextX, renderDTextY, 0xFFFFFF, true);

        // Tooltip
        List<Text> renderDTooltip = List.of(
                Text.literal(getTranslation("render_distance")).styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.literal(getTranslation("render_distance_desc")).styled(style -> style.withColor(Formatting.GRAY)),
                Text.literal(getTranslation("render_distance_desc2")).styled(style -> style.withColor(Formatting.GRAY)),
                Text.empty(),
                Text.literal(getTranslation("render_distance_disable")).styled(style -> style.withColor(Formatting.DARK_GRAY)),
                Text.literal(getTranslation("render_distance_command")).styled(style -> style.withColor(Formatting.DARK_GRAY)),
                Text.empty(),
                Text.literal(getTranslation("render_distance_current", (int) PlatformlessInitializer.maxDistance)).styled(style -> style.withColor(Formatting.YELLOW))
        );

        cY += 5 + vCH;

        // quality and qualityReset settings
        placeButton(vCH, maxSW, cY, quality, qualityReset);

        // Setting the quality text and calculating coordinates for tooltip
        Text qualityText = Text.literal(getTranslation("quality"));
        int qualityTextX = this.width / 2 - maxSW / 2;
        int qualityTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, qualityText, qualityTextX, qualityTextY, 0xFFFFFF, true);

        // Tooltip
        List<Text> qualityTooltip = List.of(
                Text.literal(getTranslation("quality")).styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.literal(getTranslation("quality_desc")).styled(style -> style.withColor(Formatting.GRAY)),
                Text.empty(),
                Text.literal(getTranslation("quality_current", screen.getQuality())).styled(style -> style.withColor(Formatting.GOLD))
        );

        cY += 15 + vCH;
        placeButton(vCH, maxSW, cY, sync, syncReset);

        // Setting the sync text and calculating coordinates for the tooltip
        Text syncText = Text.literal(getTranslation("synchronization"));
        int syncTextX = this.width / 2 - maxSW / 2;
        int syncTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, syncText, syncTextX, syncTextY, 0xFFFFFF, true);

        List<Text> syncTooltip = List.of(
                Text.literal(getTranslation("synchronization")).styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.literal(getTranslation("sync_desc")).styled(style -> style.withColor(Formatting.GRAY)),
                Text.literal(getTranslation("sync_desc2")).styled(style -> style.withColor(Formatting.GRAY)),
                Text.empty(),
                Text.literal(getTranslation("sync_current", sync.value ? getTranslation("disabled") : getTranslation("enabled"))).styled(style -> style.withColor(Formatting.GOLD))
        );

        renderTooltipIfHovered(context, mouseX, mouseY, renderDTextX, renderDTextY,
                textRenderer.getWidth(renderDText), textRenderer.fontHeight, renderDTooltip);
        renderTooltipIfHovered(context, mouseX, mouseY, qualityTextX, qualityTextY,
                textRenderer.getWidth(qualityText), textRenderer.fontHeight, qualityTooltip);
        renderTooltipIfHovered(context, mouseX, mouseY, syncTextX, syncTextY,
                textRenderer.getWidth(syncText), textRenderer.fontHeight, syncTooltip);

        // Render all child elements (buttons, sliders, etc.)
        for (Element child : children()) {
            if (child instanceof Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }

    /**
     * Places the button at the specified coordinates.
     * @param vCH the height for the button.
     * @param maxSW the maximum width of the screen.
     * @param cY the y-coordinate for placing the button.
     * @param renderD the button to be placed.
     * @param renderDReset the reset button to be placed next to the main button.
     */
    private void placeButton(int vCH, int maxSW, int cY, ClickableWidget renderD, IconButtonWidget renderDReset) {
        renderD.setX(this.width / 2 + maxSW / 2 - 80 - vCH - 5);
        renderD.setY(cY);
        renderD.setHeight(vCH);
        renderD.setWidth(80);

        renderDReset.setX(this.width / 2 + maxSW / 2 - vCH);
        renderDReset.setY(cY);
        renderDReset.setHeight(vCH);
        renderDReset.setWidth(vCH);
    }

    /**
     * Renders display screen.
     * @param context the draw context for rendering.
     */
    private void renderScreen(DrawContext context, int x, int y, int w, int h) {
        if (screen.isVideoStarted()) {
            RenderUtil2D.drawTexturedQuad(context.getMatrices(), screen.texture.getGlTexture(), x, y, w, h, screen.renderLayer);
        } else if (screen.hasPreviewTexture()) {
            RenderUtil2D.drawTexturedQuad(context.getMatrices(), screen.getPreviewTexture().getGlTexture(), x, y, w, h, screen.previewRenderLayer);
        } else {
            context.fill(x, y, x + w, y + h, 0xFF000000);
        }
    }

    /**
     * Opens the display configuration screen.
     */
    public static void open(ru.l0sty.dreamdisplays.screen.Screen screen) {
        DisplayConfScreen displayConfScreen = new DisplayConfScreen();
        displayConfScreen.setScreen(screen);
        MinecraftClient.getInstance().setScreen(displayConfScreen);
    }

    /**
     * Converts a resolution index to a quality str.
     * @param resolution the index of the resolution.
     * @return the quality string corresponding to the resolution index.
     */
    private String toQuality(int resolution) {
        List<Integer> list = screen.getQualityList();

        if (list.isEmpty()) return "144";

        int i = Math.max(Math.min(resolution, list.size() - 1), 0);
        return list.get(i).toString();
    }

    /**
     * Converts a quality string to a resolution index.
     * @param quality the quality str to convert.
     * @return the index of the resolution corresponding to the quality str.
     */
    private int fromQuality(String quality) {
        List<Integer> list = screen.getQualityList();

        if (list.isEmpty()) return 0;
        int cQ = Integer.parseInt(quality.replace("p", ""));

        int res = list.stream().filter(q -> q==cQ).findAny().orElse(Math.max(Math.min(list.getLast(), cQ), list.getFirst()));
        return list.indexOf(list.contains(res) ? res: list.getFirst());
    }

    /**
     * Sets the screen for the display config screen.
     * @param screen the screen to set.
     */
    private void setScreen(ru.l0sty.dreamdisplays.screen.Screen screen) {
        this.screen = screen;
    }
}