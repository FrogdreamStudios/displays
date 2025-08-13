package com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;


public abstract class IconButtonWidget extends AbstractButton {
	private final int iconWidth;
    private final int iconHeight;
	private final int margin;

	private ResourceLocation iconTexture;

	public void setIconTexture(ResourceLocation iconTexture) {
		this.iconTexture = iconTexture;
	}

	private WidgetSprites SPRITES = new WidgetSprites(
			ResourceLocation.parse("widget/button"), ResourceLocation.parse("widget/button_disabled"), ResourceLocation.parse("widget/button_highlighted")
	);

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

	}

	public IconButtonWidget(int x, int y, int width, int height, int iconWidth, int iconHeight, ResourceLocation iconTexture, int margin) {
		super(x, y, width, height, Component.empty());

		this.iconWidth = iconWidth;
		this.iconHeight = iconHeight;
		this.iconTexture = iconTexture;
		this.margin = margin;
	}

	public void setTextures(WidgetSprites settedTextures) {
		this.SPRITES = settedTextures;
	}

	public abstract void onPress();

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));

		int drawWidth = getWidth() - 2*margin;
		int drawHeight = getHeight() - 2*margin;

		int iconRenderWidth = drawWidth;
		int iconRenderHeight = (int) Math.max(((double) iconHeight)/iconWidth * iconRenderWidth, drawHeight);
		iconRenderWidth = (int) (((double)iconWidth)/iconHeight * iconRenderHeight);

		int drawX = getX() + getWidth()/2-iconRenderWidth/2;
		int drawY = getY() + getHeight()/2-iconRenderHeight/2;

		guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconTexture, drawX, drawY, iconRenderWidth, iconRenderHeight, ARGB.white(alpha));
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.onPress();
	}
}