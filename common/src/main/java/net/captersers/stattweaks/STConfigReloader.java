package net.captersers.stattweaks;

import net.captersers.stattweaks.manager.STBalanceManager;
import net.captersers.stattweaks.processor.EntityProcessor;
import net.captersers.stattweaks.network.ConfigSyncPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Simple hot-reload manager: reload config on server and produce a sync payload,
 * and apply a sync payload on client.
 */
public class STConfigReloader {
    private static final Logger LOGGER = LoggerFactory.getLogger("STConfigReloader");

    private STConfigReloader() {}

    /**
     * Perform reload on server: clear caches, reload configuration and return payload to broadcast.
     */
    public static ConfigSyncPayload performReload() {
        LOGGER.info("Starting StatTweaks hot-reload...");

        // Clear caches and reload server-side configuration
        STBalanceManager.reloadConfiguration();

        // Serialize current entity configurations to NBT
        CompoundTag root = new CompoundTag();

        Map<ResourceLocation, Map<String, Double>> entityMods = EntityProcessor.getEntityAttributeModifications();
        CompoundTag entitiesTag = new CompoundTag();

        for (Map.Entry<ResourceLocation, Map<String, Double>> entry : entityMods.entrySet()) {
            String entityId = entry.getKey().toString();
            CompoundTag attrTag = new CompoundTag();
            for (Map.Entry<String, Double> a : entry.getValue().entrySet()) {
                attrTag.putDouble(a.getKey(), a.getValue());
            }
            entitiesTag.put(entityId, attrTag);
        }

        root.put("entities", entitiesTag);
        root.putString("tooltip_mode", STBalanceManager.getTooltipMode());
        LOGGER.info("Serialized {} entity configurations for sync", entityMods.size());

        return new ConfigSyncPayload(root);
    }

    /**
     * Apply payload on client side: deserializes and registers configurations locally.
     * MUST be called on client main thread.
     */
    public static void applyClientSync(ConfigSyncPayload payload) {
        try {
            LOGGER.info("Applying client config sync...");
            CompoundTag root = payload.getData();
            if (root.contains("tooltip_mode")) {
                STBalanceManager.setTooltipMode(root.getString("tooltip_mode"));
            }
            if (!root.contains("entities")) {
                LOGGER.debug("No entities found in sync payload");
                return;
            }

            CompoundTag entitiesTag = root.getCompound("entities");
            // Clear existing client-side entity configs
            EntityProcessor.clearEntityAttributeModifications();

            for (String key : entitiesTag.getAllKeys()) {
                CompoundTag attrTag = entitiesTag.getCompound(key);
                Map<String, Double> attrs = new java.util.HashMap<>();
                for (String aKey : attrTag.getAllKeys()) {
                    attrs.put(aKey, attrTag.getDouble(aKey));
                }
                EntityProcessor.registerClientEntityConfiguration(ResourceLocation.parse(key), attrs);
            }

            LOGGER.info("Client applied {} entity configurations", entitiesTag.getAllKeys().size());
        } catch (Exception e) {
            LOGGER.error("Error applying client config sync: {}", e.getMessage(), e);
        }
    }
}
