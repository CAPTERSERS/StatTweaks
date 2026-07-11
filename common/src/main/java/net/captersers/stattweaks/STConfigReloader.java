package net.captersers.stattweaks;

import net.captersers.stattweaks.manager.STBalanceManager;
import net.captersers.stattweaks.network.ConfigSyncPayload;
import net.captersers.stattweaks.processor.EntityProcessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.architectury.platform.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Simple hot-reload manager: reload config on server and produce a sync payload,
 * and apply a sync payload on client.
 */
public class STConfigReloader {
    private static final Logger LOGGER = LoggerFactory.getLogger("STConfigReloader");

    private STConfigReloader() {}

    public static java.util.function.Consumer<MinecraftServer> serverRefreshCallback = (server) -> {};
    public static Runnable clientRefreshCallback = () -> {};

    /**
     * Perform reload on server: clear caches, reload configuration and return payload to broadcast.
     * 
     * @param server The MinecraftServer to refresh attributes on
     */
    public static ConfigSyncPayload performReload(MinecraftServer server) {
        LOGGER.info("Starting StatTweaks hot-reload...");

        // Clear caches and reload server-side configuration
        STBalanceManager.reloadConfiguration(server);

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

        // Add the config JSON for full synchronization (includes items, entities, etc.)
        try {
            Path configFile = Platform.getConfigFolder().resolve("CPT_StatTweaks_Config.json");
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                root.putString("config_json", json);
                LOGGER.info("Added config JSON to sync payload");
            }
        } catch (Exception e) {
            LOGGER.error("Could not read config file for sync: {}", e.getMessage());
        }

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
            
            // 1. Full config sync if available (Preferred)
            if (root.contains("config_json")) {
                String json = root.getString("config_json").orElse("");
                if (!json.isEmpty()) {
                    STBalanceManager.applyConfigurationFromJson(json);
                    LOGGER.info("Applied full configuration from server");
                }
            } else {
                // Fallback: Reload local items (Singleplayer / Legacy)
                STBalanceManager.reloadConfiguration();
            }
            
            // Trigger platform-specific client refresh
            clientRefreshCallback.run();

            // 2. Apply explicit tooltip mode if provided
            if (root.contains("tooltip_mode")) {
                STBalanceManager.setTooltipMode(root.getString("tooltip_mode").orElse("relative"));
            }

            // 3. Apply entity-specific modifications (if not already covered by JSON)
            if (root.contains("entities")) {
                CompoundTag entitiesTag = root.getCompound("entities").orElse(new CompoundTag());
                
                // If we didn't have a full JSON sync, we must clear entities before applying these
                if (!root.contains("config_json")) {
                    EntityProcessor.clearEntityAttributeModifications();
                }

                for (String key : entitiesTag.keySet()) {
                    CompoundTag attrTag = entitiesTag.getCompound(key).orElse(new CompoundTag());
                    Map<String, Double> attrs = new java.util.HashMap<>();
                    for (String aKey : attrTag.keySet()) {
                        double value = attrTag.getDouble(aKey).orElse(0.0);
                        
                        // Client-side fallback for attribute IDs
                        ResourceLocation rl = ResourceLocation.parse(aKey);
                        var holder = BuiltInRegistries.ATTRIBUTE.get(rl);
                        
                        boolean found = false;
                        if (holder.isEmpty()) {
                            String targetPath = rl.getPath().toLowerCase();
                            if (targetPath.startsWith("generic.")) {
                                targetPath = targetPath.substring(8);
                            }
                            
                            for (ResourceLocation regKey : BuiltInRegistries.ATTRIBUTE.keySet()) {
                                String keyPath = regKey.getPath().toLowerCase();
                                if (keyPath.startsWith("generic.")) {
                                    keyPath = keyPath.substring(8);
                                }
                                
                                if (keyPath.equals(targetPath)) {
                                    attrs.put(regKey.toString(), value);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!found) {
                            attrs.put(aKey, value);
                        }
                    }
                    EntityProcessor.registerClientEntityConfiguration(ResourceLocation.parse(key), attrs);
                }
                LOGGER.info("Client applied/verified {} entity configurations", entitiesTag.keySet().size());
            }
        } catch (Exception e) {
            LOGGER.error("Error applying client config sync: {}", e.getMessage(), e);
        }
    }
}
