package net.captersers.stattweaks.processor;

import net.captersers.stattweaks.config.STEntityConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Processes entity type configurations and stores attribute modifiers.
 *
 * Unlike ItemProcessor which directly modifies item instances,
 * EntityProcessor stores configurations that will be applied later
 * through platform-specific event handlers (EntityAttributeModificationEvent in NeoForge,
 * or Mixin-based interception in Fabric).
 *
 * The actual attribute application happens in platform-specific handlers because
 * the timing and mechanism differ between Fabric and NeoForge.
 */
public class EntityProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger("EntityProcessor");

    private EntityProcessor() {
        // Utility class
    }

    /**
     * Stores entity attribute modifications in memory for later application.
     * This map is populated during configuration loading and processed during
     * platform-specific attribute modification events.
     *
     * Key: Entity Type ID (e.g., "minecraft:zombie")
     * Value: Map of attribute IDs to base values
     */
    private static final Map<ResourceLocation, Map<String, Double>> ENTITY_ATTRIBUTE_MODS = new HashMap<>();

    /**
     * Registers an entity configuration for later application.
     *
     * @param entityTypeId The entity type identifier
     * @param config The entity configuration
     * @return true if the entity type exists and was registered, false otherwise
     */
    public static boolean registerEntityConfiguration(String entityTypeId, STEntityConfig config) {
        if (config == null || !config.hasModifications()) {
            return false;
        }

        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(entityTypeId);

            // Validate that the entity type exists
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(resourceLocation)) {
                LOGGER.warn("Unknown entity type '{}'. Skipping configuration.", entityTypeId);
                return false;
            }

            // Store the configuration for later application (additive)
            ENTITY_ATTRIBUTE_MODS.computeIfAbsent(resourceLocation, k -> new HashMap<>()).putAll(config.attributes);
            LOGGER.debug("Registered attribute configuration for entity type: {}", entityTypeId);
            return true;

        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid entity type identifier '{}': {}", entityTypeId, e.getMessage());
            return false;
        }
    }

    /**
     * Registers an entity configuration on the client side.
     * This allows the client to receive and store server-provided entity attribute maps.
     *
     * @param entityTypeId The entity type identifier
     * @param attributes Map of attribute ids to base values
     */
    public static void registerClientEntityConfiguration(ResourceLocation entityTypeId, Map<String, Double> attributes) {
        if (entityTypeId == null || attributes == null || attributes.isEmpty()) return;
        ENTITY_ATTRIBUTE_MODS.put(entityTypeId, new HashMap<>(attributes));
        LOGGER.debug("Client registered entity config for {}: {} attributes", entityTypeId, attributes.size());
    }

    /**
     * Retrieves all registered entity attribute modifications.
     * This is used during platform-specific attribute modification events to apply changes.
     *
     * @return An unmodifiable map of entity type IDs to attribute modifications
     */
    public static Map<ResourceLocation, Map<String, Double>> getEntityAttributeModifications() {
        return java.util.Collections.unmodifiableMap(ENTITY_ATTRIBUTE_MODS);
    }

    /**
     * Retrieves attribute modifications for a specific entity type.
     *
     * @param entityTypeId The entity type ID
     * @return An unmodifiable map of attribute IDs to values, or empty map if not found
     */
    public static Map<String, Double> getModificationsForEntityType(ResourceLocation entityTypeId) {
        Map<String, Double> mods = ENTITY_ATTRIBUTE_MODS.get(entityTypeId);
        return mods != null ? java.util.Collections.unmodifiableMap(mods) : java.util.Collections.emptyMap();
    }

    /**
     * Clears all registered entity attribute modifications.
     * Used during reloads to prevent stacking changes.
     */
    public static void clearEntityAttributeModifications() {
        ENTITY_ATTRIBUTE_MODS.clear();
        LOGGER.debug("Cleared all entity attribute modifications");
    }

    /**
     * Returns the total count of registered entity configurations.
     * Useful for logging and debugging.
     *
     * @return Number of registered entity configurations
     */
    public static int getRegisteredEntityCount() {
        return ENTITY_ATTRIBUTE_MODS.size();
    }
}
