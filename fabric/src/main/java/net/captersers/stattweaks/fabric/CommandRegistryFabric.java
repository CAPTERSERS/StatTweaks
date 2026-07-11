package net.captersers.stattweaks.fabric;

import net.captersers.stattweaks.STConfigReloader;
import net.captersers.stattweaks.network.ConfigSyncPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class CommandRegistryFabric {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("stattweaks")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        try {
                            // perform reload on server thread
                            source.getServer().execute(() -> {
                                ConfigSyncPayload payload = STConfigReloader.performReload();
                                FabricNetwork.sendConfigToAll(source.getServer(), payload);
                                source.sendSuccess(() -> Component.literal("StatTweaks: configuración recargada y sincronizada."), true);
                            });
                        } catch (Exception e) {
                            source.sendFailure(Component.literal("Error al recargar StatTweaks: " + e.getMessage()));
                        }
                        return 1;
                    })
                )
            );
        });
    }
}

