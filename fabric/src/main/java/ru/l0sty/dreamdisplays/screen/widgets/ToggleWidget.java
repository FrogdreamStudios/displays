package ru.l0sty.dreamdisplays.screen.widgets;

import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public abstract class ToggleWidget extends AbstractWidget {
	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider");
	private static final ResourceLocation HIGHLIGHTED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider_highlighted");
	private static final ResourceLocation HANDLE_TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider_handle");
	private static final ResourceLocation HANDLE_HIGHLIGHTED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");
	private double dValue;
	public boolean value;
	private boolean sliderFocused;

	public ToggleWidget(int x, int y, int width, int height, Component text, boolean value) {
		super(x, y, width, height, text);
		this.dValue = value ? 1 : 0;
		this.value = value;
	}

	private ResourceLocation getTexture() {
		return this.isFocused() && !this.sliderFocused ? HIGHLIGHTED_TEXTURE : TEXTURE;
	}

	private ResourceLocation getHandleTexture() {
		return !this.isHovered && !this.sliderFocused ? HANDLE_TEXTURE : HANDLE_HIGHLIGHTED_TEXTURE;
	}

	@Override
	protected MutableComponent createNarrationMessage() {
		return Component.translatable("gui.narrate.slider", this.getMessage());
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput builder) {

	}

	@Override
	public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
		Minecraft minecraftClient = Minecraft.getInstance();
		context.blitSprite(RenderType::guiTextured, this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
		context.blitSprite(RenderType::guiTextured, this.getHandleTexture(), this.getX() + (int)(this.dValue * (double)(this.width - 8)), this.getY(), 8, this.getHeight());

		int i = this.active ? 16777215 : 10526880;
		this.renderScrollingString(context, minecraftClient.font, 2, i | Mth.ceil(this.alpha * 255.0F) << 24);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.setValueFromMouse();
		applyValue();
		updateMessage();
	}

	@Override
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

    /**
	 * Sets the value from mouse position.
	 * 
	 * <p>The value will be calculated from the position and the width of this
	 * slider.
	 *
     */
	private void setValueFromMouse() {
		value = !value;
		dValue = value ? 1 : 0;
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public void onRelease(double mouseX, double mouseY) {
		super.playDownSound(Minecraft.getInstance().getSoundManager());
	}

	protected abstract void updateMessage();

	protected abstract void applyValue();
}