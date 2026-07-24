package com.pythemcio;

import com.pythemcio.command.PythemcioCommand;
import com.pythemcio.event.EventRegistry;
import com.pythemcio.event.PlayerStateTracker;
import net.fabricmc.api.ClientModInitializer;

public class PythemcIOClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PythemcIO.LOGGER.info("[PythemcIO] Client initializing...");

        PythemcioCommand.register();
        EventRegistry.register();
        PlayerStateTracker.register();

        PythemcIO.LOGGER.info("[PythemcIO] Client initialized.");
    }
}
