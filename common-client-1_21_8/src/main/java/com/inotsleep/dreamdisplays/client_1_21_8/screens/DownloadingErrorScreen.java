package com.inotsleep.dreamdisplays.client_1_21_8.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DownloadingErrorScreen extends Screen {
    Font font = Minecraft.getInstance().font;


    public DownloadingErrorScreen() {
        super(Component.translatable("dreamdisplays.ui.downloader-error.title"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int currentY = height / 4;

        guiGraphics.drawCenteredString(font, title, width / 2, currentY, 0xFFFFFF);
        currentY += font.lineHeight * 2;
        guiGraphics.drawCenteredString(font, Component.translatable("dreamdisplays.ui.downloader-error.subtitle"), width / 2, currentY, 0xFFFFFF);
    }
}
