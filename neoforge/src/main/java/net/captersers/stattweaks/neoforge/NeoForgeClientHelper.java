package net.captersers.stattweaks.neoforge;

import net.captersers.stattweaks.STConfigReloader;
import net.captersers.stattweaks.manager.STBalanceManager;
import net.minecraft.client.Minecraft;

/**
 * Client-only utility to avoid ClassNotFoundException on dedicated servers.
 */
public class NeoForgeClientHelper {
    public static void init() {
        STConfigReloader.clientRefreshCallback = NeoForgeClientHelper::refreshClientState;
    }

    public static void refreshClientState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            STBalanceManager.refreshLivingEntityEquipment(mc.player);
        }
        if (mc.level != null) {
            STBalanceManager.refreshEntitiesInLevel(mc.level.entitiesForRendering());
        }
    }
}
