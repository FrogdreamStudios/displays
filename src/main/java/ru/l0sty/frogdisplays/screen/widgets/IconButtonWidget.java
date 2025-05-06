package ru.l0sty.frogdisplays.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import ru.l0sty.frogdisplays.render.RenderUtil2D;

/**
 * A pressable widget has a press action. It is pressed when it is clicked. It is
 * also pressed when enter or space keys are pressed when it is selected.
 */
@Environment(EnvType.CLIENT)
public abstract class IconButtonWidget extends ClickableWidget {
	private int iw, ih;
	private int margin;

	private Identifier iconTexture;
	private Identifier backgroundTexture;

	public void setIconTexture(Identifier iconTexture) {
		this.iconTexture = iconTexture;
	}

	protected static final int field_43050 = 2;
	private static final ButtonTextures TEXTURES = new ButtonTextures(
		Identifier.ofVanilla("widget/button"), Identifier.ofVanilla("widget/button_disabled"), Identifier.ofVanilla("widget/button_highlighted")
	);
	
	private ButtonTextures settedTextures = null;

	public IconButtonWidget(int i, int j, int k, int l, int iw, int ih, Identifier iconTexture, int margin) {
		super(i, j, k, l, Text.empty());

		this.iw = iw;
		this.ih = ih;
		this.iconTexture = iconTexture;
		this.margin = margin;
	}

	public void setTextures(ButtonTextures settedTextures) {
		this.settedTextures = settedTextures;
	}

	public abstract void onPress();

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		context.drawGuiTexture(RenderLayer::getGuiTextured, settedTextures != null ? settedTextures.get(this.active, this.isSelected()) : TEXTURES.get(this.active, this.isSelected()), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ColorHelper.getWhite(this.alpha));

		int dW = getWidth() - 2*margin;
		int dH = getHeight() - 2*margin;

		int iconW = dW;
		int iconH = (int) Math.max(((double) ih)/iw * iconW, dH);
		iconW = (int) (((double)iw)/ih * iconH);

		int dx = getX() + getWidth()/2-iconW/2;
		int dy = getY() + getHeight()/2-iconH/2;

		//RenderUtil2D.drawScaledTexture(context, iconTexture, dx, dy, iconW, iconH);
		context.drawGuiTexture(RenderLayer::getGuiTextured, iconTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), ColorHelper.getWhite(this.alpha));

	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.onPress();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!this.active || !this.visible) {
			return false;
		} else if (KeyCodes.isToggle(keyCode)) {
			this.playDownSound(MinecraftClient.getInstance().getSoundManager());
			this.onPress();
			return true;
		} else {
			return false;
		}
	}
}
