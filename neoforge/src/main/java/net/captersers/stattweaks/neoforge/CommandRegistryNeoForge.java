package net.captersers.stattweaks.neoforge;

import net.captersers.stattweaks.STConfigReloader;
import net.captersers.stattweaks.Stattweaks;
import net.captersers.stattweaks.network.ConfigSyncPayload;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registry for NeoForge-specific commands.
 */
@EventBusSubscriber(modid = Stattweaks.MOD_ID)
public final class CommandRegistryNeoForge {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        Stattweaks.LOGGER.info("Registering StatTweaks commands for NeoForge...");
        event.getDispatcher().register(
            Commands.literal("stattweaks")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        // perform reload on server thread to ensure thread safety
                        source.getServer().execute(() -> {
                            try {
                                ConfigSyncPayload payload = STConfigReloader.performReload(source.getServer());
                                NeoForgeNetwork.sendConfigToAll(source.getServer(), payload);
                                source.sendSuccess(() -> Component.literal("StatTweaks: configuración recargada y sincronizada."), true);
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Error al recargar StatTweaks: " + e.getMessage()));
                                e.printStackTrace();
                            }
                        });
                        return 1;
                    })
                )
        );
    }
}
