package ru.l0sty.dreamdisplays.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

/**
 * Abstract class for a button with an icon.
 * This class extends ClickableWidget and provides functionality for rendering an icon button with a specified texture.
 * It allows setting a custom icon texture and provides methods for rendering the button and handling click events.
 */
public abstract class IconButtonWidget extends AbstractWidget {
	private final int iw;
    private final int ih;
	private final int margin;

	private ResourceLocation iconTexture;

	public void setIconTexture(ResourceLocation iconTexture) {
		this.iconTexture = iconTexture;
	}

	private static final WidgetSprites TEXTURES = new WidgetSprites(
		ResourceLocation.withDefaultNamespace("widget/button"), ResourceLocation.withDefaultNamespace("widget/button_disabled"), ResourceLocation.withDefaultNamespace("widget/button_highlighted")
	);
	
	private WidgetSprites settedTextures = null;

	public IconButtonWidget(int i, int j, int k, int l, int iw, int ih, ResourceLocation iconTexture, int margin) {
		super(i, j, k, l, Component.empty());

		this.iw = iw;
		this.ih = ih;
		this.iconTexture = iconTexture;
		this.margin = margin;
	}

	public void setTextures(WidgetSprites settedTextures) {
		this.settedTextures = settedTextures;
	}

	public abstract void onPress();

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {}

	@Override
	protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
		context.blitSprite(RenderType::guiTextured, settedTextures != null ? settedTextures.get(this.active, this.isHoveredOrFocused()) : TEXTURES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));

		int dW = getWidth() - 2*margin;
		int dH = getHeight() - 2*margin;

		int iconW = dW;
		int iconH = (int) Math.max(((double) ih)/iw * iconW, dH);
		iconW = (int) (((double)iw)/ih * iconH);

		int dx = getX() + getWidth()/2-iconW/2;
		int dy = getY() + getHeight()/2-iconH/2;

		context.blitSprite(RenderType::guiTextured, iconTexture, dx, dy, iconW, iconH, ARGB.white(this.alpha));

	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.onPress();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!this.active || !this.visible) {
			return false;
		} else if (CommonInputs.selected(keyCode)) {
			this.playDownSound(Minecraft.getInstance().getSoundManager());
			this.onPress();
			return true;
		} else {
			return false;
		}
	}
}