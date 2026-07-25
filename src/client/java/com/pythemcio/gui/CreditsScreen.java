package com.pythemcio.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CreditsScreen extends Screen {

    private final Screen parent;

    public CreditsScreen(Screen parent) {
        super(Component.literal("Credits"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> minecraft.setScreen(parent)
        ).bounds(width / 2 - 40, height - 40, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.drawCenteredString(font, ChatFormatting.BOLD + "PythemcIO", width / 2, 30, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, "v" + net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("pythemcio").get().getMetadata().getVersion().getFriendlyString(), width / 2, 44, 0x888888);
        guiGraphics.drawCenteredString(font, "", width / 2, 58, 0xAAAAAA);

        int y = 70;
        guiGraphics.drawCenteredString(font, ChatFormatting.AQUA + "Author: " + ChatFormatting.WHITE + "AntacidDT", width / 2, y, 0xFFFFFFFF);
        y += 14;
        guiGraphics.drawCenteredString(font, ChatFormatting.AQUA + "License: " + ChatFormatting.WHITE + "Apache-2.0", width / 2, y, 0xFFFFFFFF);
        y += 14;
        guiGraphics.drawCenteredString(font, ChatFormatting.AQUA + "Released: " + ChatFormatting.WHITE + "24.07.2026", width / 2, y, 0xFFFFFFFF);
        y += 14;
        guiGraphics.drawCenteredString(font, ChatFormatting.AQUA + "GitHub: " + ChatFormatting.WHITE + "github.com/AntacidDT/PythemcIO", width / 2, y, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
