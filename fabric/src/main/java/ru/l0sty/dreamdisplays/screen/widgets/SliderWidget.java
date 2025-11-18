package ru.l0sty.dreamdisplays.screen.widgets;

import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public abstract class SliderWidget extends AbstractWidget {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider");
    private static final ResourceLocation HIGHLIGHTED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider_highlighted");
    private static final ResourceLocation HANDLE_TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider_handle");
    private static final ResourceLocation HANDLE_HIGHLIGHTED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");
    public double value;
    private boolean sliderFocused;

    public SliderWidget(int x, int y, int width, int height, Component text, double value) {
        super(x, y, width, height, text);
        this.value = value;
    }

    private ResourceLocation getTexture() {
        return this.isFocused() && !this.sliderFocused ? HIGHLIGHTED_TEXTURE : TEXTURE;
    }

    private ResourceLocation getHandleTexture() {
        return !this.isHovered && !this.sliderFocused ? HANDLE_TEXTURE : HANDLE_HIGHLIGHTED_TEXTURE;
    }

    protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.slider", this.getMessage());
    }

    public void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                builder.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.focused"));
            } else {
                builder.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.hovered"));
            }
        }

    }

    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        context.blitSprite(RenderType::guiTextured, this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        context.blitSprite(RenderType::guiTextured, this.getHandleTexture(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
        int i = this.active ? 16777215 : 10526880;
        this.renderScrollingString(context, minecraftClient.font, 2, i | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    public void onClick(double mouseX, double mouseY) {
        this.setValueFromMouse(mouseX);
    }

    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.sliderFocused = false;
        } else {
            InputType guiNavigationType = Minecraft.getInstance().getLastInputType();
            if (guiNavigationType == InputType.MOUSE || guiNavigationType == InputType.KEYBOARD_TAB) {
                this.sliderFocused = true;
            }

        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CommonInputs.selected(keyCode)) {
            this.sliderFocused = !this.sliderFocused;
            return true;
        } else {
            if (this.sliderFocused) {
                boolean bl = keyCode == 263;
                if (bl || keyCode == 262) {
                    float f = bl ? -1.0F : 1.0F;
                    this.setValue(this.value + (double)(f / (float)(this.width - 8)));
                    return true;
                }
            }

            return false;
        }
    }

    private void setValueFromMouse(double mouseX) {
        this.setValue((mouseX - (double)(this.getX() + 4)) / (double)(this.width - 8));
    }

    private void setValue(double value) {
        double d = this.value;
        this.value = Mth.clamp(value, 0.0, 1.0);
        if (d != this.value) {
            this.applyValue();
        }

        this.updateMessage();
    }

    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.setValueFromMouse(mouseX);
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    public void playDownSound(SoundManager soundManager) {
    }

    public void onRelease(double mouseX, double mouseY) {
        super.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    protected abstract void updateMessage();

    protected abstract void applyValue();
}