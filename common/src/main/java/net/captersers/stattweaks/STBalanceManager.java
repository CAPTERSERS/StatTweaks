package net.captersers.stattweaks;

import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import net.captersers.stattweaks.mixin.StatAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core manager for dynamic item statistic modification.
 * <p>
 * This class handles the serialization/deserialization of JSON configurations
 * and injects the new values directly into the {@link DataComponentMap} of items
 * at runtime.
 * <p>
 * It includes a snapshot system to allow hot-reloading (via {@code /reload}) without
 * permanently corrupting the item registry state.
 */
public class STBalanceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ItemBalance");



    /** * Unique identifier for attributes applied by this mod.
     * Used to distinguish between vanilla attributes and custom tweaks during cleanup.
     */
    private static final ResourceLocation BALANCE_ID = ResourceLocation.parse("balance:custom_stats");



    /**
     * In-memory cache of original item components before modification.
     * Essential for restoring state during data pack reloads.
     */
    private static final Map<Item, DataComponentMap> ORIGINAL_COMPONENTS = new HashMap<>();

    // Visualization and serialization helpers
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CONFIG_TYPE = new TypeToken<Map<String, Map<String, Double>>>(){}.getType();



    /** * Mapping between JSON config keys and Minecraft Attribute registries.
     */
    private static final Map<String, Holder<Attribute>> STAT_TO_ATTRIBUTE = Map.of(
            "damage", Attributes.ATTACK_DAMAGE,
            "speed", Attributes.ATTACK_SPEED,
            "reach", Attributes.ENTITY_INTERACTION_RANGE,
            "knockback", Attributes.ATTACK_KNOCKBACK,
            "armor", Attributes.ARMOR,
            "toughness", Attributes.ARMOR_TOUGHNESS,
            "knockback_resistance", Attributes.KNOCKBACK_RESISTANCE,
            "movement_speed", Attributes.MOVEMENT_SPEED
    );

    // Optimized set for quick key validation during parsing
    private static final Set<String> VALID_KEYS = new HashSet<>();
    static {
        VALID_KEYS.addAll(STAT_TO_ATTRIBUTE.keySet());
        VALID_KEYS.add("durability");
        VALID_KEYS.add("efficiency");
        VALID_KEYS.add("stack_size");
    }



    /**
     * Initializes the manager.
     * Registers lifecycle events and reload listeners to handle configuration loading.
     */
    public static void init() {
        // Initial load during server startup
        LifecycleEvent.SETUP.register(STBalanceManager::loadConfiguration);

        // Register listener for /reload command (Data Pack reload)
        ReloadListenerRegistry.register(PackType.SERVER_DATA, (ResourceManagerReloadListener) resourceManager -> {
            LOGGER.info("Reload detected. Purging applied stats and reloading config...");

            // Critical: Revert items to vanilla state before applying new configs
            // to prevent "stacking" modifiers on every reload.
            restoreOriginalValues();
            loadConfiguration();
        });
    }



    /**
     * Reads the {@code item_stats.json} file from the config directory.
     * Iterates through tags and individual items to apply changes.
     */
    private static void loadConfiguration() {
        Path configDir = Platform.getConfigFolder();
        File file = configDir.resolve("CPT_StatTweaks_Config.json").toFile();

        // Generate default config if missing
        if (!file.exists()) {
            try (Writer writer = new FileWriter(file)) {
                writer.write("{}");
                LOGGER.info("Created default configuration file at {}", file.getPath());
            } catch (Exception e) {
                LOGGER.error("Failed to generate default config file", e);
            }
        }

        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            Map<String, Map<String, Double>> config = GSON.fromJson(reader, CONFIG_TYPE);

            if (config != null && !config.isEmpty()) {
                LOGGER.info("Parsing {} configuration entries...", config.size());

                // Phase 1: Process Tags (General application)
                // e.g., #minecraft:swords
                config.forEach((key, stats) -> {
                    if (key.startsWith("#")) {
                        processEntry(key, stats, true);
                    }
                });

                // Phase 2: Process Specific Items (Overrides)
                // e.g., minecraft:diamond_sword
                config.forEach((key, stats) -> {
                    if (!key.startsWith("#")) {
                        processEntry(key, stats, false);
                    }
                });
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load item statistics configuration", e);
        }
    }



    /**
     * Resolves the target (Item or Tag) and delegates the application of stats.
     *
     * @param key   The registry name or tag key string.
     * @param stats The map of statistics to apply.
     * @param isTag True if the key represents a TagKey.
     */
    private static void processEntry(String key, Map<String, Double> stats, boolean isTag) {
        try {
            if (isTag) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(key.substring(1)));
                // Iterate all items currently in the tag
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                    applyStats(holder.value(), stats);
                }
            } else {
                ResourceLocation id = ResourceLocation.parse(key);
                if (BuiltInRegistries.ITEM.containsKey(id)) {
                    applyStats(BuiltInRegistries.ITEM.get(id), stats);
                } else {
                    LOGGER.warn("Skipping unknown item identifier: {}", key);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while processing entry: " + key, e);
        }
    }




    /**
     * The core logic for modifying an item's Data Components.
     * <p>
     * This method utilizes the {@link StatAccessor} mixin to access the typically
     * protected or immutable component map of the Item class.
     *
     * @param item  The item instance to modify.
     * @param stats The values from the JSON config.
     */
    private static void applyStats(Item item, Map<String, Double> stats) {
        StatAccessor accessor = (StatAccessor) item;

        // Create a snapshot of the vanilla state if not already cached
        if (!ORIGINAL_COMPONENTS.containsKey(item)) {
            ORIGINAL_COMPONENTS.put(item, accessor.getComponents());
        }

        // Components are immutable, so we must build a new map based on the current one
        DataComponentMap currentMap = accessor.getComponents();
        DataComponentMap.Builder newMap = DataComponentMap.builder();
        newMap.addAll(currentMap);

        // Validation: Warn about unsupported keys
        for (String key : stats.keySet()) {
            if (!VALID_KEYS.contains(key)) {
                LOGGER.warn("Unknown statistic key '{}' for item {}", key, item);
            }
        }

        // Determine appropriate equipment slot for attribute modifiers
        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.MAINHAND;
        if (item instanceof ArmorItem armorItem) {
            slotGroup = switch (armorItem.getType()) {
                case HELMET -> EquipmentSlotGroup.HEAD;
                case CHESTPLATE -> EquipmentSlotGroup.CHEST;
                case LEGGINGS -> EquipmentSlotGroup.LEGS;
                case BOOTS -> EquipmentSlotGroup.FEET;
                default -> EquipmentSlotGroup.ARMOR;
            };
        }

        // 1. Modify Durability
        if (stats.containsKey("durability")) {
            newMap.set(DataComponents.MAX_DAMAGE, Math.max(1, stats.get("durability").intValue()));
        }

        // 2. Modify Mining Speed (Efficiency)
        if (stats.containsKey("efficiency") && currentMap.has(DataComponents.TOOL)) {
            Tool oldTool = currentMap.get(DataComponents.TOOL);
            // Reconstruct the Tool component with new speed, preserving rules and damage per block
            newMap.set(DataComponents.TOOL, new Tool(oldTool.rules(), stats.get("efficiency").floatValue(), oldTool.damagePerBlock()));
        }

        // 3. Modify Max Stack Size
        if (stats.containsKey("stack_size")) {
            int newStackSize = stats.get("stack_size").intValue();
            // Clamp to vanilla limits to ensure stability
            newStackSize = Math.max(1, Math.min(99, newStackSize));
            newMap.set(DataComponents.MAX_STACK_SIZE, newStackSize);
        }

        // 4. Attribute Modifiers (Damage, Speed, Armor, etc.)
        Set<Holder<Attribute>> attributesToUpdate = new HashSet<>();
        for (String key : stats.keySet()) {
            if (STAT_TO_ATTRIBUTE.containsKey(key)) {
                attributesToUpdate.add(STAT_TO_ATTRIBUTE.get(key));
            }
        }

        if (!attributesToUpdate.isEmpty()) {
            ItemAttributeModifiers originalModifiers = currentMap.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, item.getDefaultAttributeModifiers());
            ItemAttributeModifiers.Builder attrBuilder = ItemAttributeModifiers.builder();

            // Retain vanilla modifiers that are NOT being overridden by our config
            for (ItemAttributeModifiers.Entry entry : originalModifiers.modifiers()) {
                boolean isCustomModifier = entry.modifier().id().equals(BALANCE_ID);
                boolean isTargetAttribute = attributesToUpdate.contains(entry.attribute());

                if (!isCustomModifier && !isTargetAttribute) {
                    attrBuilder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            }

            // Inject new custom modifiers
            for (Map.Entry<String, Holder<Attribute>> entry : STAT_TO_ATTRIBUTE.entrySet()) {
                String jsonKey = entry.getKey();
                Holder<Attribute> attribute = entry.getValue();

                if (stats.containsKey(jsonKey)) {
                    double rawValue = stats.get(jsonKey);
                    // Convert raw JSON value to internal Minecraft modifier value
                    double finalValue = calculateInternalValue(jsonKey, rawValue);

                    // Choose operation type
                    // Most attributes are ADD_VALUE (flat addition)
                    AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;

                    // Percentage-based attributes require MULTIPLIED_BASE
                    if (jsonKey.equals("movement_speed") || jsonKey.equals("knockback_resistance")) {
                        operation = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    }

                    attrBuilder.add(attribute,
                            new AttributeModifier(BALANCE_ID, finalValue, operation),
                            slotGroup);
                }
            }
            newMap.set(DataComponents.ATTRIBUTE_MODIFIERS, attrBuilder.build());
        }

        // Inject the newly built component map into the item
        accessor.setComponents(newMap.build());
    }



    /**
     * Reverts all modified items to their original DataComponents state.
     * This is crucial to prevent duplicate modifier stacking during reloading.
     */
    private static void restoreOriginalValues() {
        if (ORIGINAL_COMPONENTS.isEmpty()) return;

        LOGGER.info("Restoring {} items to vanilla state...", ORIGINAL_COMPONENTS.size());

        for (Map.Entry<Item, DataComponentMap> entry : ORIGINAL_COMPONENTS.entrySet()) {
            Item item = entry.getKey();
            DataComponentMap originalComponents = entry.getValue();

            // Restore via Mixin Accessor
            ((StatAccessor) item).setComponents(originalComponents);
        }
    }



    /**
     * Converts user-friendly JSON values to Minecraft's internal attribute logic.
     * <p>
     * Example: Minecraft calculates attack damage as {@code Base + Modifier}.
     * Since the player's base hand damage is 1, a sword with 7 total damage
     * needs a modifier of +6.
     *
     * @param key       The attribute key.
     * @param jsonValue The value defined in the JSON file.
     * @return The corrected value for the AttributeModifier.
     */
    private static double calculateInternalValue(String key, double jsonValue) {
        return switch (key) {
            case "damage" -> Math.max(0, jsonValue) - 1.0; // Compensate for base fist damage (1.0)
            case "speed" -> jsonValue - 4.0;               // Compensate for base attack speed (4.0)
            case "reach" -> jsonValue - 3.0;               // Compensate for base reach (~3.0)
            default -> jsonValue;                          // Armor, Toughness, etc. match 1:1
        };
    }
}