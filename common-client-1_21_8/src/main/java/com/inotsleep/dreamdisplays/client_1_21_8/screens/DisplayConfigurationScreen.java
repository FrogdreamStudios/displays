package com.inotsleep.dreamdisplays.client_1_21_8.screens;

import com.inotsleep.dreamdisplays.client.ClientModHolder;
import com.inotsleep.dreamdisplays.client.Config;
import com.inotsleep.dreamdisplays.client.DisplayManager;
import com.inotsleep.dreamdisplays.client.display.Display;
import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import com.inotsleep.dreamdisplays.client_1_21_8.render.TextureObject;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets.IconButtonWidget;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets.ToggleSlider;
import com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets.ValueAccessibleSlider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class DisplayConfigurationScreen extends Screen {
    ValueAccessibleSlider volume = null;
    ValueAccessibleSlider renderDistance = null;
    ValueAccessibleSlider quality = null;
    ToggleSlider sync = null;

    IconButtonWidget backButton = null;
    IconButtonWidget forwardButton = null;
    IconButtonWidget pauseButton = null;

    IconButtonWidget renderDistanceReset = null;
    IconButtonWidget qualityReset = null;
    IconButtonWidget syncReset = null;

    IconButtonWidget deleteButton = null;
    IconButtonWidget reportButton = null;
    
    Display display;

    protected void init() {
        volume = new ValueAccessibleSlider(0, 0, 0, 0, Component.literal((int) Math.floor(display.getVolume() * 100) + "%"), display.getVolume()) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal((int) Math.floor(value * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                display.setVolume((float) value);
            }
        };

        backButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "button_seek_backwards_icon"), 2) {
            @Override
            public void onPress() {
                display.seekBackward();
            }
        };

        forwardButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "button_seek_forward_icon"), 2) {
            @Override
            public void onPress() {
                display.seekForward();
            }
        };

        pauseButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, display.isPaused() ? "button_unpause_icon" : "button_pause_icon"), 2) {
            @Override
            public void onPress() {
                display.setPaused(!display.isPaused());
                setIconTexture(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, display.isPaused() ? "button_unpause_icon" : "button_pause_icon"));
            }
        };
        renderDistance = new ValueAccessibleSlider(0, 0, 0, 0, Component.literal(String.valueOf(Config.getInstance().renderDistance)), (double) (Config.getInstance().renderDistance - 24) /(96-24)) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(String.valueOf((int) (value*(96-24)) + 24)));
            }

            @Override
            protected void applyValue() {
                Config.getInstance().renderDistance = (int) (value * (96-24) + 24);
            }
        };

        quality = new ValueAccessibleSlider(0, 0, 0, 0, Component.literal(display.getQuality()+"p"), ((double) fromQuality(display.getQuality())) / display.getAvailableQualities().size()) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(toQuality((int) (value*display.getAvailableQualities().size()))+"p"));
            }

            @Override
            protected void applyValue() {
                setMessage(Component.literal(toQuality((int) (value*display.getAvailableQualities().size()))+"p"));
            }
        };

        renderDistanceReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "button_reset_icon"), 2) {
            @Override
            public void onPress() {
                Config.getInstance().renderDistance = 64;
                renderDistance.setValue((double) 40/72);
                renderDistance.setMessage(Component.literal("64"));
            }
        };

        qualityReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "button_reset_icon"), 2) {
            @Override
            public void onPress() {
                display.setQuality(Config.getInstance().defaultQuality);
                quality.setValue((double) fromQuality(Config.getInstance().defaultQuality) / display.getAvailableQualities().size());
                quality.setMessage(Component.literal(toQuality(Config.getInstance().defaultQuality) + "p"));
            }
        };

        sync = new ToggleSlider(0, 0, 0, 0, display.isSync()) {
            @Override
            protected void applyValue() {
                if (display.isOwner()) {
                    display.setSync(value);
                    syncReset.active = value;
                    display.executeAfterInit(display::sendSyncUpdate);
                }
            }
        };

        syncReset = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "button_reset_icon"), 2) {

            @Override
            public void onPress() {
                if (display.isOwner()) {
                    sync.value = false;
                    display.executeAfterInit(display::sendSyncUpdate);
                }
            }
        };

        sync.active = display.isOwner();

        deleteButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "delete"), 2) {
            @Override
            public void onPress() {
                ClientModHolder.getInstance().sendDeletePacket(display.getId());
                onClose();
            }
        };

        deleteButton.active = display.isOwner();

        reportButton = new IconButtonWidget(0, 0, 0, 0, 64, 64, ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "report"), 2) {
            @Override
            public void onPress() {
                ClientModHolder.getInstance().sendReportPacket(display.getId());
                onClose();
            }
        };

        WidgetSprites textures = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "widgets/red_button"), ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "widgets/red_button_disabled"), ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, "widgets/red_button_highlighted"));

        deleteButton.setTextures(textures);
        reportButton.setTextures(textures);

        addWidget(volume);
        addWidget(backButton);
        addWidget(forwardButton);
        addWidget(pauseButton);
        addWidget(renderDistance);
        addWidget(quality);
        addWidget(qualityReset);
        addWidget(renderDistanceReset);
        addWidget(sync);
        addWidget(syncReset);
        addWidget(deleteButton);
        addWidget(reportButton);
    }
    
    public DisplayConfigurationScreen(Display display) {
        super(Component.translatable("dreamdisplays.ui.title"));
        this.display = display;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        assert minecraft != null;
        Font textRenderer = minecraft.font;

        Component headerText = Component.translatable("dreamdisplays.ui.title");

        int componentHeight = 25;

        deleteButton.setX(10);
        deleteButton.setY(this.height - componentHeight - 10);
        deleteButton.setHeight(componentHeight);
        deleteButton.setWidth(componentHeight);

        reportButton.setX(this.width - componentHeight - 10);
        reportButton.setY(this.height - componentHeight - 10);
        reportButton.setHeight(componentHeight);
        reportButton.setWidth(componentHeight);

        syncReset.active = display.isOwner() && display.isSync();
        renderDistanceReset.active = Config.getInstance().renderDistance != 64;
        qualityReset.active = Config.getInstance().defaultQuality == display.getQuality();

        int headerTextWidth = textRenderer.width(headerText);
        int headerTextX = (this.width - headerTextWidth) / 2;
        int headerTextY = 15;
        guiGraphics.drawString(textRenderer, headerText, headerTextX, headerTextY, 0xFFFFFFFF, true);

        int maxDisplayWidth = this.width / 3;

        int displayHeight = (int) Math.min((double) display.getHeight() / display.getWidth() * maxDisplayWidth, this.height / 3.5);
        maxDisplayWidth = (int) ((double) display.getWidth() / display.getHeight() * displayHeight);
        int displayX = this.width / 2 - maxDisplayWidth / 2;
        int currentY = textRenderer.lineHeight + 15 * 2;

        guiGraphics.fill(this.width / 2 - maxDisplayWidth / 2, currentY, this.width / 2 + maxDisplayWidth / 2, currentY + displayHeight, 0xff000000);

        TextureObject textureObject = DisplayManager.getData(TextureObject.class, display.getId());
        if (textureObject != null) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, textureObject.getTexture(), displayX, currentY, maxDisplayWidth, displayHeight);

        currentY += displayHeight;
        currentY += 5;

        volume.setX(this.width / 2 - maxDisplayWidth / 2);
        volume.setY(currentY);
        volume.setHeight(componentHeight);
        volume.setWidth(Math.min(maxDisplayWidth / 3, maxDisplayWidth / 2 - componentHeight * 9 / 8 - 5));

        backButton.setX(this.width / 2 - componentHeight * 9 / 8);
        backButton.setY(currentY);
        backButton.setHeight(componentHeight);
        backButton.setWidth(componentHeight);

        forwardButton.setX(this.width / 2 + componentHeight / 8);
        forwardButton.setY(currentY);
        forwardButton.setHeight(componentHeight);
        forwardButton.setWidth(componentHeight);

        pauseButton.setX(this.width / 2 + maxDisplayWidth / 2 - componentHeight);
        pauseButton.setY(currentY);
        pauseButton.setHeight(componentHeight);
        pauseButton.setWidth(componentHeight);

        backButton.active = !(display.isSync() && !display.isOwner());
        forwardButton.active = !(display.isSync() && !display.isOwner());
        pauseButton.active = !(display.isSync() && !display.isOwner());

        sync.active = (display.isOwner());
        deleteButton.active = (display.isOwner());

        currentY += 10 + componentHeight;

        placeButton(componentHeight, maxDisplayWidth, currentY, renderDistance, renderDistanceReset);

        Component renderDistanceText = Component.translatable("dreamdisplays.button.render-distance");
        int renderDistanceTextX = this.width / 2 - maxDisplayWidth / 2;
        int renderDistanceTextY = currentY + componentHeight / 2 - textRenderer.lineHeight / 2;
        guiGraphics.drawString(textRenderer, renderDistanceText, renderDistanceTextX, renderDistanceTextY, 0xFFFFFFFF, true);

        List<Component> renderDistanceTooltip = List.of(
                Component.translatable("dreamdisplays.button.render-distance.tooltip.1").withStyle(style -> style.withColor(ChatFormatting.WHITE).withBold(true)),
                Component.translatable("dreamdisplays.button.render-distance.tooltip.2").withStyle(style -> style.withColor(ChatFormatting.GRAY)),
                Component.translatable("dreamdisplays.button.render-distance.tooltip.3").withStyle(style -> style.withColor(ChatFormatting.GRAY)),
                Component.translatable("dreamdisplays.button.render-distance.tooltip.4"),
                Component.translatable("dreamdisplays.button.render-distance.tooltip.5").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)),
                Component.translatable("dreamdisplays.button.render-distance.tooltip.6").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)),
                Component.translatable("dreamdisplays.button.render-distance.tooltip.7"),
                Component.translatable("dreamdisplays.button.render-distance.tooltip.8", Config.getInstance().renderDistance).withStyle(style -> style.withColor(ChatFormatting.YELLOW))
        );

        currentY += 5 + componentHeight;

        placeButton(componentHeight, maxDisplayWidth, currentY, quality, qualityReset);

        Component qualityText = Component.translatable("dreamdisplays.button.quality");
        int qualityTextX = this.width / 2 - maxDisplayWidth / 2;
        int qualityTextY = currentY + componentHeight / 2 - textRenderer.lineHeight / 2;
        guiGraphics.drawString(textRenderer, qualityText, qualityTextX, qualityTextY, 0xFFFFFFFF, true);

        List<Component> qualityTooltip = List.of(
                Component.translatable("dreamdisplays.button.quality.tooltip.1").withStyle(style -> style.withColor(ChatFormatting.WHITE).withBold(true)),
                Component.translatable("dreamdisplays.button.quality.tooltip.2").withStyle(style -> style.withColor(ChatFormatting.GRAY)),
                Component.translatable("dreamdisplays.button.quality.tooltip.3"),
                Component.translatable("dreamdisplays.button.quality.tooltip.4", display.getQuality()+"p").withStyle(style -> style.withColor(ChatFormatting.GOLD))
        );

        currentY += 15 + componentHeight;
        placeButton(componentHeight, maxDisplayWidth, currentY, sync, syncReset);

        Component syncText = Component.translatable("dreamdisplays.button.synchronization");
        int syncTextX = this.width / 2 - maxDisplayWidth / 2;
        int syncTextY = currentY + componentHeight / 2 - textRenderer.lineHeight / 2;
        guiGraphics.drawString(textRenderer, syncText, syncTextX, syncTextY, 0xFFFFFFFF, true);

        List<Component> syncTooltip = List.of(
                Component.translatable("dreamdisplays.button.synchronization.tooltip.1").withStyle(style -> style.withColor(ChatFormatting.WHITE).withBold(true)),
                Component.translatable("dreamdisplays.button.synchronization.tooltip.2").withStyle(style -> style.withColor(ChatFormatting.GRAY)),
                Component.translatable("dreamdisplays.button.synchronization.tooltip.3").withStyle(style -> style.withColor(ChatFormatting.GRAY)),
                Component.translatable("dreamdisplays.button.synchronization.tooltip.4"),
                Component.translatable("dreamdisplays.button.synchronization.tooltip.5", display.isSync() ? Component.translatable("dreamdisplays.button.enabled") : Component.translatable("dreamdisplays.button.disabled")).withStyle(style -> style.withColor(ChatFormatting.GOLD))
        );

        renderTooltipIfHovered(guiGraphics, mouseX, mouseY, renderDistanceTextX, renderDistanceTextY,
                textRenderer.width(renderDistanceText), textRenderer.lineHeight, renderDistanceTooltip);
        renderTooltipIfHovered(guiGraphics, mouseX, mouseY, qualityTextX, qualityTextY,
                textRenderer.width(qualityText), textRenderer.lineHeight, qualityTooltip);
        renderTooltipIfHovered(guiGraphics, mouseX, mouseY, syncTextX, syncTextY,
                textRenderer.width(syncText), textRenderer.lineHeight, syncTooltip);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderTooltipIfHovered(GuiGraphics context, int mouseX, int mouseY,
                                        int elementX, int elementY, int elementWidth, int elementHeight,
                                        List<Component> tooltip) {
        if (mouseX >= elementX && mouseX <= elementX + elementWidth &&
                mouseY >= elementY && mouseY <= elementY + elementHeight) {
            context.renderTooltip(font, tooltip.stream().map(mutableComponent -> ClientTooltipComponent.create(mutableComponent.getVisualOrderText())).toList(), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    private void placeButton(int componentHeight, int maxDisplayWidth, int currentY, AbstractWidget widget, AbstractWidget resetWidget) {
        widget.setX(this.width / 2 + maxDisplayWidth / 2 - 80 - componentHeight - 5);
        widget.setY(currentY);
        widget.setHeight(componentHeight);
        widget.setWidth(80);

        resetWidget.setX(this.width / 2 + maxDisplayWidth / 2 - componentHeight);
        resetWidget.setY(currentY);
        resetWidget.setHeight(componentHeight);
        resetWidget.setWidth(componentHeight); // reset widgets are squares.
    }

    private String toQuality(int resolution) {
        List<Integer> list = display.getAvailableQualities();

        if (list.isEmpty()) return "144";

        int i = Math.max(Math.min(resolution, list.size() - 1), 0);
        return list.get(i).toString();
    }

    private int fromQuality(int quality) {
        List<Integer> list = display.getAvailableQualities();

        if (list.isEmpty()) return 0;

        int res = list.stream().filter(q -> q==quality).findAny().orElse(Math.max(Math.min(list.getLast(), quality), list.getFirst()));
        return list.indexOf(list.contains(res) ? res: list.getFirst());
    }
}
