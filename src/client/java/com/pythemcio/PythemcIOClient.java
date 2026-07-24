package com.pythemcio;

import com.pythemcio.command.PythemcioCommand;
import com.pythemcio.event.EventRegistry;
import com.pythemcio.event.PlayerStateTracker;
import com.pythemcio.server.ApiServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class PythemcIOClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PythemcIO.LOGGER.info("[PythemcIO] Client initializing...");

        PythemcioCommand.register();
        EventRegistry.register();
        PlayerStateTracker.register();

        ApiServer.start(FabricLoader.getInstance().getGameDir().resolve("config").resolve("pythemcio"));

        PythemcIO.LOGGER.info("[PythemcIO] Client initialized.");
    }
}
