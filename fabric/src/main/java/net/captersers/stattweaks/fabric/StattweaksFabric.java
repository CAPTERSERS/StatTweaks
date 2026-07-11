package net.captersers.stattweaks.fabric;

import net.captersers.stattweaks.Stattweaks;
import net.fabricmc.api.ModInitializer;

public final class StattweaksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Initialize Fabric networking
        FabricNetwork.init();

        // Initialize Fabric-specific entity attribute handler
        FabricEntityAttributeHandler.init();

        // Register commands and network handlers
        CommandRegistryFabric.init();

        // Run our common setup
        Stattweaks.init();
    }
}
