package net.captersers.stattweaks.neoforge;

import net.captersers.stattweaks.Stattweaks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Stattweaks.MOD_ID)
public final class StattweaksNeoForge {
    public StattweaksNeoForge(IEventBus modEventBus) {
        // Initialize NeoForge-specific entity handler with the mod event bus
        NeoForgeEntityAttributeHandler.init(modEventBus);

        // Register network payloads
        modEventBus.addListener(NeoForgeNetwork::register);

        // Register common setup
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Run our common setup
        Stattweaks.init();
    }
}
