package com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public abstract class ToggleSlider extends AbstractSliderButton {
    public boolean value;

    public ToggleSlider(int x, int y, int width, int height, boolean value) {
        super(x, y, width, height, getLabel(value), value ? 1 : 0);
        this.value = value;
    }

    private static Component getLabel(boolean value) {
        return Component.translatable(value ? "dreamdisplays.button.enabled": "dreamdisplays.button.disabled");
    }

    @Override
    protected void updateMessage() {
        setMessage(getLabel(value));
    }


}
