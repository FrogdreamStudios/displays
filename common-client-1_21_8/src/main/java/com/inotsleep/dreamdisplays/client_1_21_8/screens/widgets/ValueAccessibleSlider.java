package com.inotsleep.dreamdisplays.client_1_21_8.screens.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public abstract class ValueAccessibleSlider extends AbstractSliderButton {
    public ValueAccessibleSlider(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message, value);
    }

    public void setValue(double value) {
        this.value = value;
    }
}
