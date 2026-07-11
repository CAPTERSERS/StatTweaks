package net.captersers.stattweaks.neoforge;

import net.captersers.stattweaks.Stattweaks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

@Mod(Stattweaks.MOD_ID)
public final class StattweaksNeoForge {
    public StattweaksNeoForge(IEventBus modEventBus) {
        // Initialize NeoForge-specific entity handler with the mod event bus
        NeoForgeEntityAttributeHandler.init(modEventBus);

        // Register network payloads
        modEventBus.addListener(NeoForgeNetwork::register);

        // Register common setup
        modEventBus.addListener(this::commonSetup);
        
        // Register client setup if on client
        if (Platform.getEnv().isClient()) {
            modEventBus.addListener(this::clientSetup);
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        NeoForgeClientHelper.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Run our common setup
        Stattweaks.init();
    }
}
