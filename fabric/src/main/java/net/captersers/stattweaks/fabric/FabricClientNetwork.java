package net.captersers.stattweaks.fabric;

import net.captersers.stattweaks.STConfigReloader;
import net.captersers.stattweaks.manager.STBalanceManager;
import net.captersers.stattweaks.network.ConfigSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side networking registration for Fabric
 */
public final class FabricClientNetwork {
    public static void init() {
        STConfigReloader.clientRefreshCallback = () -> {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client.player != null) {
                STBalanceManager.refreshLivingEntityEquipment(client.player);
            }
            if (client.level != null) {
                STBalanceManager.refreshEntitiesInLevel(client.level.entitiesForRendering());
            }
        };

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
            // Ensure running on client main thread
            context.client().execute(() -> {
                STConfigReloader.applyClientSync(payload);
            });
        });
    }
}