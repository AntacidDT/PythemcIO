package com.pythemcio;

import com.pythemcio.trigger.TriggerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythemcIO implements ModInitializer {
    public static final String MOD_ID = "pythemcio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[PythemcIO] Initializing...");

        TriggerManager.init(FabricLoader.getInstance().getGameDir());

        LOGGER.info("[PythemcIO] Initialized.");
    }
}
