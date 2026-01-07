package net.captersers.stattweaks;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Stattweaks {
    public static final String MOD_ID = "stattweaks";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Initializing StatTweaks mod...");

        STBalanceManager.init();
    }
}
