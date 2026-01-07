package net.captersers.stattweaks.fabric;

import net.captersers.stattweaks.Stattweaks;
import net.fabricmc.api.ModInitializer;

public final class StattweaksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Run our common setup.
        Stattweaks.init();
    }
}
