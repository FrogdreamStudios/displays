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
        super(Text.translatable("dreamdisplays.ui.title"));
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

        renderD = new SliderWidget(0, 0, 0, 0, Text.of(String.valueOf((int) PlatformlessInitializer.config.maxDistance)), (PlatformlessInitializer.config.maxDistance-24)/(96-24)) {
            @Override
            protected void updateMessage() {
                setMessage(Text.of(String.valueOf((int) (value*(96-24)) + 24)));
            }

            @Override
            protected void applyValue() {
                PlatformlessInitializer.config.maxDistance = (int) (value * (96-24) + 24);
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
                PlatformlessInitializer.config.maxDistance = 64;
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

        sync = new ToggleWidget(0, 0, 0, 0, Text.translatable(screen.isSync ? "dreamdisplays.button.enabled" : "dreamdisplays.button.disabled"), screen.isSync) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable(screen.isSync ? "dreamdisplays.button.enabled" : "dreamdisplays.button.disabled"));
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
        Text headerText = Text.translatable("dreamdisplays.ui.title");

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
                    Text.translatable("dreamdisplays.error.loadingerror.1").styled(style -> style.withColor(0xff0000)),
                    Text.translatable("dreamdisplays.error.loadingerror.2").styled(style -> style.withColor(0xff0000)),
                    Text.translatable("dreamdisplays.error.loadingerror.3").styled(style -> style.withColor(0xff0000)),
                    Text.translatable("dreamdisplays.error.loadingerror.4").styled(style -> style.withColor(0xff0000)),
                    Text.translatable("dreamdisplays.error.loadingerror.5").styled(style -> style.withColor(0xff0000))
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
        renderDReset.active = PlatformlessInitializer.config.maxDistance != 64;
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
        Text renderDText = Text.translatable("dreamdisplays.button.render-distance");
        int renderDTextX = this.width / 2 - maxSW / 2;
        int renderDTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, renderDText, renderDTextX, renderDTextY, 0xFFFFFF, true);

        // Tooltip
        List<Text> renderDTooltip = List.of(
                Text.translatable("dreamdisplays.button.render-distance.tooltip.1").styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.translatable("dreamdisplays.button.render-distance.tooltip.2").styled(style -> style.withColor(Formatting.GRAY)),
                Text.translatable("dreamdisplays.button.render-distance.tooltip.3").styled(style -> style.withColor(Formatting.GRAY)),
                Text.translatable("dreamdisplays.button.render-distance.tooltip.4"),
                Text.translatable("dreamdisplays.button.render-distance.tooltip.5").styled(style -> style.withColor(Formatting.DARK_GRAY)),
                Text.translatable("dreamdisplays.button.render-distance.tooltip.6").styled(style -> style.withColor(Formatting.DARK_GRAY)),
                Text.translatable("dreamdisplays.button.render-distance.tooltip.7"),
                Text.translatable("dreamdisplays.button.render-distance.tooltip.8", (int) PlatformlessInitializer.config.maxDistance).styled(style -> style.withColor(Formatting.YELLOW))
        );

        cY += 5 + vCH;

        // quality and qualityReset settings
        placeButton(vCH, maxSW, cY, quality, qualityReset);

        // Setting the quality text and calculating coordinates for tooltip
        Text qualityText = Text.translatable("dreamdisplays.button.quality");
        int qualityTextX = this.width / 2 - maxSW / 2;
        int qualityTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, qualityText, qualityTextX, qualityTextY, 0xFFFFFF, true);

        // Tooltip
        List<Text> qualityTooltip = List.of(
                Text.translatable("dreamdisplays.button.quality.tooltip.1").styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.translatable("dreamdisplays.button.quality.tooltip.2").styled(style -> style.withColor(Formatting.GRAY)),
                Text.translatable("dreamdisplays.button.quality.tooltip.3"),
                Text.translatable("dreamdisplays.button.quality.tooltip.4", screen.getQuality()).styled(style -> style.withColor(Formatting.GOLD))
        );

        cY += 15 + vCH;
        placeButton(vCH, maxSW, cY, sync, syncReset);

        // Setting the sync text and calculating coordinates for the tooltip
        Text syncText = Text.translatable("dreamdisplays.button.synchronization");
        int syncTextX = this.width / 2 - maxSW / 2;
        int syncTextY = cY + vCH / 2 - textRenderer.fontHeight / 2;
        context.drawText(textRenderer, syncText, syncTextX, syncTextY, 0xFFFFFF, true);

        List<Text> syncTooltip = List.of(
                Text.translatable("dreamdisplays.button.synchronization.tooltip.1").styled(style -> style.withColor(Formatting.WHITE).withBold(true)),
                Text.translatable("dreamdisplays.button.synchronization.tooltip.2").styled(style -> style.withColor(Formatting.GRAY)),
                Text.translatable("dreamdisplays.button.synchronization.tooltip.3").styled(style -> style.withColor(Formatting.GRAY)),
                Text.translatable("dreamdisplays.button.synchronization.tooltip.4"),
                Text.translatable("dreamdisplays.button.synchronization.tooltip.5", sync.value ? Text.translatable("dreamdisplays.button.enabled") : Text.translatable("dreamdisplays.button.disabled")).styled(style -> style.withColor(Formatting.GOLD))
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
