package ru.l0sty.frogdisplays.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public abstract class ToggleWidget extends ClickableWidget {
	private static final Identifier TEXTURE = Identifier.ofVanilla("widget/slider");
	private static final Identifier HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/slider_highlighted");
	private static final Identifier HANDLE_TEXTURE = Identifier.ofVanilla("widget/slider_handle");
	private static final Identifier HANDLE_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/slider_handle_highlighted");
	protected static final int field_43054 = 2;
	private static final int field_41790 = 8;
	private static final int field_41789 = 4;
	private double dValue;
	public boolean value;
	private boolean sliderFocused;

	public ToggleWidget(int x, int y, int width, int height, Text text, boolean value) {
		super(x, y, width, height, text);
		this.dValue = value ? 1 : 0;
		this.value = value;
	}

	private Identifier getTexture() {
		return this.isFocused() && !this.sliderFocused ? HIGHLIGHTED_TEXTURE : TEXTURE;
	}

	private Identifier getHandleTexture() {
		return !this.hovered && !this.sliderFocused ? HANDLE_TEXTURE : HANDLE_HIGHLIGHTED_TEXTURE;
	}

	@Override
	protected MutableText getNarrationMessage() {
		return Text.translatable("gui.narrate.slider", this.getMessage());
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {

	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		context.drawGuiTexture(RenderLayer::getGuiTextured, this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
		context.drawGuiTexture(RenderLayer::getGuiTextured, this.getHandleTexture(), this.getX() + (int)(this.dValue * (double)(this.width - 8)), this.getY(), 8, this.getHeight());

		int i = this.active ? 16777215 : 10526880;
		this.drawScrollableText(context, minecraftClient.textRenderer, 2, i | MathHelper.ceil(this.alpha * 255.0F) << 24);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.setValueFromMouse(mouseX);
		applyValue();
		updateMessage();
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.sliderFocused = false;
		} else {
			GuiNavigationType guiNavigationType = MinecraftClient.getInstance().getNavigationType();
			if (guiNavigationType == GuiNavigationType.MOUSE || guiNavigationType == GuiNavigationType.KEYBOARD_TAB) {
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
	private void setValueFromMouse(double mouseX) {
		value = !value;
		dValue = value ? 1 : 0;
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public void onRelease(double mouseX, double mouseY) {
		super.playDownSound(MinecraftClient.getInstance().getSoundManager());
	}

	protected abstract void updateMessage();

	protected abstract void applyValue();
}
