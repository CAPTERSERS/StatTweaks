package net.captersers.stattweaks.manager;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import net.captersers.stattweaks.config.STConfig;
import net.captersers.stattweaks.config.STConditionalModifier;
import net.captersers.stattweaks.config.STEntityConfig;
import net.captersers.stattweaks.config.STItemConfig;
import net.captersers.stattweaks.loader.ConfigLoader;
import net.captersers.stattweaks.mixin.StatAccessor;
import net.captersers.stattweaks.processor.EntityProcessor;
import net.captersers.stattweaks.processor.ItemProcessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Core manager for dynamic item statistic modification.
 *
 * Responsibilities:
 * - Coordinates configuration loading via ConfigLoader
 * - Applies item configurations via ItemProcessor
 * - Manages snapshot system for hot-reloading
 * - Handles tag-based and item-specific configurations
 * - Provides access to conditional modifiers for the STConditionalAttributeApplier
 *
 * Architecture:
 * - ConfigLoader: Reads and parses JSON → STConfig
 * - ItemProcessor: Applies STConfig to Items
 * - STBalanceManager: Orchestrates the flow, manages state
 */
public class STBalanceManager {
    /**
     * Current tooltip display mode.
     */
    private static String tooltipMode = "relative";

    public static String getTooltipMode() {
        return tooltipMode;
    }

    public static void setTooltipMode(String mode) {
        tooltipMode = mode;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("STBalanceManager");

    /**
     * In-memory cache of original item components before modification.
     * Essential for restoring state during data pack reloads.
     * Key: Item, Value: Original DataComponentMap
     */
    private static final Map<Item, DataComponentMap> ORIGINAL_COMPONENTS = new HashMap<>();

    /**
     * Stores conditional modifiers for items.
     * Key: Item, Value: Map of condition names to STConditionalModifier objects.
     */
    private static final Map<Item, Map<String, STConditionalModifier>> CONDITIONAL_MODIFIERS = new HashMap<>();

    /**
     * Stores the current server instance for reloads.
     */
    private static MinecraftServer currentServer = null;

    private STBalanceManager() {
        // Utility class, no instantiation
    }

    /**
     * Initializes the balance manager.
     * Registers lifecycle events and reload listeners.
     */
    public static void init() {
        LOGGER.info("Initializing STBalanceManager...");

        // Store server instance when it starts
        LifecycleEvent.SERVER_STARTED.register(server -> currentServer = server);
        LifecycleEvent.SERVER_STOPPED.register(server -> currentServer = null);

        // Initial load during server startup
        LifecycleEvent.SETUP.register(() -> loadAndApplyConfiguration());

        // Register listener for /reload command (Data Pack reload)
        ReloadListenerRegistry.register(PackType.SERVER_DATA, (ResourceManagerReloadListener) resourceManager -> {
            LOGGER.info("Data pack reload detected. Restoring items to vanilla state and reloading config...");

            // Critical: Revert items to vanilla state before applying new configs
            // to prevent "stacking" modifiers on every reload.
            restoreItemsToVanilla();
            loadAndApplyConfiguration();
            
            if (currentServer != null) {
                refreshAll(currentServer);
            }
        }, ResourceLocation.fromNamespaceAndPath("stattweaks", "config_reloader"));
    }

    /**
     * Public entry point to reload configuration at runtime (hot-reload).
     * Restores items to vanilla state and then loads+applies the configuration again.
     */
    public static void reloadConfiguration() {
        reloadConfiguration(null);
    }

    /**
     * Public entry point to reload configuration at runtime (hot-reload).
     * Restores items to vanilla state and then loads+applies the configuration again.
     * Intended to be called from command handlers.
     *
     * @param server The MinecraftServer to refresh attributes on
     */
    public static void reloadConfiguration(MinecraftServer server) {
        LOGGER.info("Reloading StatTweaks configuration via reloadConfiguration(server)...");
        restoreItemsToVanilla();
        loadAndApplyConfiguration();
        
        if (server != null) {
            refreshAll(server);
        }
    }

    /**
     * Refreshes attributes for all online players and loaded entities.
     * This ensures that changes are reflected immediately.
     */
    public static void refreshAll(MinecraftServer server) {
        LOGGER.info("Refreshing all players and entities on server...");
        
        for (ServerLevel level : server.getAllLevels()) {
            refreshEntitiesInLevel(level.getAllEntities());
        }
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayerEquipment(player);
        }
    }

    /**
     * Refreshes all living entities in a given level.
     */
    public static void refreshEntitiesInLevel(Iterable<? extends Entity> entities) {
        int count = 0;
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                EntityProcessor.applyToEntity(living);
                count++;
            }
        }
        if (count > 0) {
            LOGGER.debug("Refreshed {} entities in level", count);
        }
    }

    /**
     * Forces a refresh of all equipment-based attribute modifiers for a living entity.
     */
    public static void refreshLivingEntityEquipment(LivingEntity entity) {
        if (entity == null) return;
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                    // Force refresh by setting the same item again (triggers attribute update)
                    entity.setItemSlot(slot, stack.copy());
                }
            }
        }
    }

    /**
     * Forces a refresh of all equipment-based attribute modifiers for a player.
     * Alias for refreshLivingEntityEquipment.
     */
    private static void refreshPlayerEquipment(LivingEntity player) {
        refreshLivingEntityEquipment(player);
    }

    /**
     * Applies the configuration from a JSON string.
     * Used for synchronizing from server to client.
     *
     * @param json The JSON string to parse and apply
     */
    public static void applyConfigurationFromJson(String json) {
        LOGGER.info("Applying configuration from provided JSON...");
        restoreItemsToVanilla();
        STConfig config = ConfigLoader.parseConfigString(json);
        if (config != null) {
            applyConfiguration(config);
        }
    }

    /**
     * Loads the configuration and applies it to all configured items and entities.
     */
    private static void loadAndApplyConfiguration() {
        try {
            // Load configuration using ConfigLoader
            STConfig config = ConfigLoader.loadConfiguration(Platform.getConfigFolder());
            if (config != null) {
                applyConfiguration(config);
            } else {
                LOGGER.warn("Configuration is null, skipping application.");
            }
        } catch (Exception e) {
            LOGGER.error("Error during configuration load: {}", e.getMessage(), e);
        }
    }

    /**
     * Shared logic to apply a loaded configuration object.
     */
    private static void applyConfiguration(STConfig config) {
        try {
            if (config == null || !config.isValid()) {
                LOGGER.warn("Configuration is empty or invalid. No items or entities will be modified.");
                return;
            }

            tooltipMode = config.tooltipMode != null ? config.tooltipMode : "relative";
            LOGGER.info("Applying configuration ({} items, {} entities). Tooltip mode: {}",
                    config.items.size(), config.entities.size(), tooltipMode);

            // First pass: Process tags (General application)
            processTagConfigurations(config);
            processEntityTagConfigurations(config);

            // Second pass: Process specific entries (these will override tag settings if conflicts exist)
            processItemConfigurations(config);
            processEntityConfigurations(config);

            LOGGER.info("Configuration application complete.");

        } catch (Exception e) {
            LOGGER.error("Error applying configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes tag-based configurations.
     *
     * Tags start with '#' and apply the configuration to all items matching the tag.
     * Example: "#minecraft:axes"
     *
     * @param config The configuration to process
     */
    private static void processTagConfigurations(STConfig config) {
        for (Map.Entry<String, STItemConfig> entry : config.items.entrySet()) {
            String key = entry.getKey();
            STItemConfig itemConfig = entry.getValue();

            if (key.startsWith("#")) {
                try {
                    ResourceLocation tagId = ResourceLocation.parse(key.substring(1));
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);

                    int itemsInTag = 0;
                    var tagOptional = BuiltInRegistries.ITEM.get(tagKey);

                    if (tagOptional.isPresent()) {
                        for (var itemHolder : tagOptional.get()) {
                            Item item = itemHolder.value();
                            if (item != net.minecraft.world.item.Items.AIR) {
                                if (ItemProcessor.applyItemConfiguration(item, itemConfig, ORIGINAL_COMPONENTS)) {
                                    storeConditionalModifiers(item, itemConfig);
                                    itemsInTag++;
                                }
                            }
                        }
                    } else {
                        LOGGER.warn("Tag '{}' not found in registry (might not be loaded yet or is empty)", key);
                    }

                    if (itemsInTag > 0) {
                        LOGGER.info("Applied configuration from tag '{}' to {} items", key, itemsInTag);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing tag entry '{}': {}", key, e.getMessage());
                }
            }
        }
    }

    /**
     * Processes specific item configurations.
     *
     * These are applied after tags, so they can override tag configurations.
     *
     * @param config The configuration to process
     */
    private static void processItemConfigurations(STConfig config) {
        for (Map.Entry<String, STItemConfig> entry : config.items.entrySet()) {
            String key = entry.getKey();
            STItemConfig itemConfig = entry.getValue();

            if (!key.startsWith("#")) {
                try {
                    // Validate configuration before processing
                    if (!ConfigLoader.validateItemConfig(key, itemConfig)) {
                        continue;
                    }

                    ResourceLocation itemId = ResourceLocation.parse(key);
                    Item item = BuiltInRegistries.ITEM.get(itemId).map(Holder::value).orElse(net.minecraft.world.item.Items.AIR);

                    if (item != net.minecraft.world.item.Items.AIR) {
                        if (ItemProcessor.applyItemConfiguration(item, itemConfig, ORIGINAL_COMPONENTS)) {
                            storeConditionalModifiers(item, itemConfig);
                            LOGGER.info("Applied configuration to item: {}", key);
                        }
                    } else {
                        LOGGER.warn("Skipping unknown item identifier: {}", key);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing item entry '{}': {}", key, e.getMessage());
                }
            }
        }
    }

    /**
     * Processes entity type configurations.
     *
     * @param config The configuration to process
     */
    private static void processEntityConfigurations(STConfig config) {
        if (config.entities == null || config.entities.isEmpty()) {
            return;
        }

        int successCount = 0;

        for (Map.Entry<String, STEntityConfig> entry : config.entities.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("#")) continue; // Skip tags in this pass

            STEntityConfig entityConfig = entry.getValue();
            try {
                if (EntityProcessor.registerEntityConfiguration(key, entityConfig)) {
                    successCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Error processing entity entry '{}': {}", key, e.getMessage());
            }
        }

        if (successCount > 0) {
            LOGGER.info("Registered configurations for {} entity types", successCount);
        }
    }

    /**
     * Processes entity tag configurations.
     *
     * @param config The configuration to process
     */
    private static void processEntityTagConfigurations(STConfig config) {
        if (config.entities == null || config.entities.isEmpty()) {
            return;
        }

        int tagCount = 0;

        for (Map.Entry<String, STEntityConfig> entry : config.entities.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("#")) continue;

            STEntityConfig entityConfig = entry.getValue();
            try {
                ResourceLocation tagId = ResourceLocation.parse(key.substring(1));
                TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagId);

                var tagOptional = BuiltInRegistries.ENTITY_TYPE.get(tagKey);
                if (tagOptional.isPresent()) {
                    int entitiesInTag = 0;
                    for (var entityTypeHolder : tagOptional.get()) {
                        String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityTypeHolder.value()).toString();
                        if (EntityProcessor.registerEntityConfiguration(entityTypeId, entityConfig)) {
                            entitiesInTag++;
                        }
                    }
                    if (entitiesInTag > 0) {
                        LOGGER.info("Applied configuration from entity tag '{}' to {} entity types", key, entitiesInTag);
                        tagCount++;
                    }
                } else {
                    LOGGER.warn("Entity tag '{}' not found", key);
                }
            } catch (Exception e) {
                LOGGER.error("Error processing entity tag '{}': {}", key, e.getMessage());
            }
        }
    }

    /**
     * Stores conditional modifiers for later use by STConditionalAttributeApplier.
     *
     * @param item The item to store modifiers for
     * @param itemConfig The configuration containing the modifiers
     */
    private static void storeConditionalModifiers(Item item, STItemConfig itemConfig) {
        if (itemConfig.conditions != null && !itemConfig.conditions.isEmpty()) {
            CONDITIONAL_MODIFIERS.put(item, itemConfig.conditions);
            LOGGER.debug("Stored {} conditional modifiers for item {}", itemConfig.conditions.size(), item);
        } else {
            // If an item previously had conditions but no longer does, remove them
            CONDITIONAL_MODIFIERS.remove(item);
        }
    }

    /**
     * Restores all modified items to their original vanilla state.
     *
     * This is essential for hot-reloading to prevent duplicate modifier stacking.
     */
    private static void restoreItemsToVanilla() {
        if (ORIGINAL_COMPONENTS.isEmpty()) {
            LOGGER.debug("No items to restore.");
            return;
        }

        LOGGER.info("Restoring {} items to vanilla state...", ORIGINAL_COMPONENTS.size());

        for (Map.Entry<Item, DataComponentMap> entry : ORIGINAL_COMPONENTS.entrySet()) {
            Item item = entry.getKey();
            DataComponentMap originalComponents = entry.getValue();

            try {
                ((StatAccessor) item).setComponents(originalComponents);
            } catch (Exception e) {
                LOGGER.error("Error restoring item {}: {}", item, e.getMessage());
            }
        }

        ORIGINAL_COMPONENTS.clear();
        CONDITIONAL_MODIFIERS.clear();
        EntityProcessor.clearEntityAttributeModifications();
    }

    /**
     * Provides access to the stored conditional modifiers.
     *
     * This is intended for use by the STConditionalAttributeApplier.
     *
     * @return An unmodifiable map of items to their conditional modifiers
     */
    public static Map<Item, Map<String, STConditionalModifier>> getConditionalModifiers() {
        return java.util.Collections.unmodifiableMap(CONDITIONAL_MODIFIERS);
    }
}
