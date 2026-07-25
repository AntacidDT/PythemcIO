package com.pythemcio;

import com.pythemcio.command.PythemcioCommand;
import com.pythemcio.event.EventRegistry;
import com.pythemcio.event.PlayerStateTracker;
import com.pythemcio.gui.PythemcioConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PythemcIOClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PythemcIO.LOGGER.info("[PythemcIO] Client initializing...");

        PythemcioCommand.register();
        EventRegistry.register();
        PlayerStateTracker.register();
        registerPauseScreenButton();

        PythemcIO.LOGGER.info("[PythemcIO] Client initialized. Use /pythemcio enable i to start API server.");
    }

    private void registerPauseScreenButton() {
        ScreenEvents.AFTER_INIT.register((minecraft, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen pauseScreen && pauseScreen.showsPauseMenu()) {
                List<AbstractWidget> buttons = Screens.getButtons(screen);

                int bottomY = 0;
                int btnWidth = 200;
                for (AbstractWidget widget : buttons) {
                    int widgetBottom = widget.getY() + widget.getHeight();
                    if (widgetBottom > bottomY) {
                        bottomY = widgetBottom;
                        btnWidth = widget.getWidth();
                    }
                }

                int buttonY = bottomY + 4;
                int centerX = scaledWidth / 2;

                Button pythemcioButton = Button.builder(
                    Component.literal("PythemcIO"),
                    btn -> minecraft.setScreen(new PythemcioConfigScreen(screen))
                ).bounds(centerX - btnWidth / 2, buttonY, btnWidth, 20).build();

                buttons.add(pythemcioButton);
            }
        });
    }
}
