package net.captersers.stattweaks.processor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.captersers.stattweaks.config.STItemConfig;
import net.captersers.stattweaks.mixin.StatAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Processes item configurations and applies attribute modifiers and data components.
 */
public class ItemProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("ItemProcessor");
    private static final Gson GSON = new Gson();

    private ItemProcessor() {
        // Utility class
    }

    /**
     * Applies the given configuration to an item.
     *
     * @param item The item to modify
     * @param itemConfig The configuration to apply
     * @param originalComponentsSnapshot Snapshot of original components for restoration
     * @return true if applied successfully, false otherwise
     */
    public static boolean applyItemConfiguration(Item item, STItemConfig itemConfig, Map<Item, DataComponentMap> originalComponentsSnapshot) {
        if (item == null || itemConfig == null) {
            return false;
        }

        try {
            StatAccessor accessor = (StatAccessor) item;
            
            // Store original components if not already stored
            if (!originalComponentsSnapshot.containsKey(item)) {
                originalComponentsSnapshot.put(item, accessor.getComponents());
            }

            // Start with the CURRENT components (enables additive tags/configs)
            DataComponentMap currentMap = accessor.getComponents();
            DataComponentMap.Builder newMapBuilder = DataComponentMap.builder();
            newMapBuilder.addAll(currentMap);

            // Apply attributes (including special ones like durability, efficiency)
            if (itemConfig.attributes != null && !itemConfig.attributes.isEmpty()) {
                injectAttributeModifiers(item, itemConfig.attributes, newMapBuilder, currentMap);
            }

            // Apply data components (rarity, food, fire_resistant, etc.)
            if (itemConfig.components != null && !itemConfig.components.isEmpty()) {
                applyDataComponents(item, itemConfig.components, newMapBuilder);
            }

            // Apply the new component map to the item
            accessor.setComponents(newMapBuilder.build());
            return true;

        } catch (Exception e) {
            LOGGER.error("Error applying configuration to item {}: {}", item, e.getMessage());
            return false;
        }
    }

    /**
     * Injects attribute modifiers into the item's component map.
     */
    private static void injectAttributeModifiers(Item item, Map<String, Double> attributes, DataComponentMap.Builder newMapBuilder, DataComponentMap currentMap) {
        ItemAttributeModifiers originalModifiers = currentMap.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (originalModifiers == null) {
            originalModifiers = ItemAttributeModifiers.EMPTY;
        }

        ItemAttributeModifiers.Builder attrBuilder = ItemAttributeModifiers.builder();
        Map<Holder<Attribute>, Double> standardAttributes = new HashMap<>();
        Set<ResourceLocation> overriddenAttributes = new HashSet<>();
        
        // Handle special attributes and resolve standard ones
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();

            if ("stattweaks:durability".equals(key)) {
                newMapBuilder.set(DataComponents.MAX_DAMAGE, (int) value);
            } else if ("stattweaks:efficiency".equals(key)) {
                applyToolEfficiency(item, value, newMapBuilder, currentMap);
            } else {
                Holder<Attribute> attribute = resolveAttribute(key);
                if (attribute != null) {
                    standardAttributes.put(attribute, value);
                    attribute.unwrapKey().ifPresent(k -> overriddenAttributes.add(k.location()));
                }
            }
        }

        // Determine equipment slot based on item type
        EquipmentSlotGroup slotGroup = determineEquipmentSlot(item);

        // Retain modifiers that are NOT being overridden
        for (ItemAttributeModifiers.Entry entry : originalModifiers.modifiers()) {
            ResourceLocation entryAttrLoc = entry.attribute().unwrapKey()
                    .map(net.minecraft.resources.ResourceKey::location)
                    .orElse(null);
            
            boolean isOverridden = false;
            if (entryAttrLoc != null) {
                isOverridden = overriddenAttributes.contains(entryAttrLoc);
            } else {
                // Fallback for direct holders if key is missing
                isOverridden = standardAttributes.containsKey(entry.attribute());
            }

            if (!isOverridden) {
                attrBuilder.add(entry.attribute(), entry.modifier(), entry.slot());
            } else {
                LOGGER.debug("Overriding vanilla modifier for {} on {}", entryAttrLoc, item);
            }
        }

        // Add new/overridden modifiers
        for (Map.Entry<Holder<Attribute>, Double> entry : standardAttributes.entrySet()) {
            Holder<Attribute> attribute = entry.getKey();
            double targetValue = entry.getValue();
            
            double internalValue = calculateInternalValue(item, attribute, targetValue);
            AttributeModifier.Operation operation = determineOperation(attribute);

            // Generate a unique ID for this modifier
            ResourceLocation modifierId = generateModifierId(item, attribute);

            attrBuilder.add(attribute,
                    new AttributeModifier(modifierId, internalValue, operation),
                    slotGroup);
            
            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                LOGGER.info("Item {}: Applied attack damage modifier {} (Total Target: {})", item, internalValue, internalValue + 1.0);
            }
        }

        ItemAttributeModifiers finalModifiers = attrBuilder.build();
        newMapBuilder.set(DataComponents.ATTRIBUTE_MODIFIERS, finalModifiers);
        LOGGER.info("Applied {} attribute modifiers to {}", standardAttributes.size(), item);
    }

    /**
     * Specialized logic for tool efficiency.
     */
    private static void applyToolEfficiency(Item item, double value, DataComponentMap.Builder newMapBuilder, DataComponentMap currentMap) {
        Tool tool = currentMap.get(DataComponents.TOOL);
        if (tool != null) {
            List<Tool.Rule> newRules = new ArrayList<>();
            for (Tool.Rule rule : tool.rules()) {
                newRules.add(new Tool.Rule(rule.blocks(), Optional.of((float) value), rule.correctForDrops()));
            }
            newMapBuilder.set(DataComponents.TOOL, new Tool(newRules, (float) value, tool.damagePerBlock(), true));
            LOGGER.info("Updated tool efficiency for {}: {} rules", item, newRules.size());
        }
    }

    /**
     * Injects arbitrary data components from a JSON map.
     */
    private static void applyDataComponents(Item item, Map<String, Object> components, DataComponentMap.Builder newMapBuilder) {
        for (Map.Entry<String, Object> entry : components.entrySet()) {
            String componentIdString = entry.getKey();
            Object jsonValue = entry.getValue();

            try {
                ResourceLocation componentId = ResourceLocation.parse(componentIdString);
                
                // Try to get the component from the registry. 
                // In some versions Registry.get returns Optional, in others it returns T.
                // Our previous fixes suggest it returns Optional<Holder.Reference<T>> in this environment.
                DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(componentId)
                        .map(Holder::value)
                        .orElse(null);

                if (componentType == null) {
                    // Try to find it by path only (case-insensitive fallback)
                    String targetPath = componentId.getPath().toLowerCase();
                    LOGGER.warn("Component '{}' not found. Searching registry (size: {})...", componentIdString, BuiltInRegistries.DATA_COMPONENT_TYPE.keySet().size());
                    
                    for (ResourceLocation key : BuiltInRegistries.DATA_COMPONENT_TYPE.keySet()) {
                        String keyPath = key.getPath().toLowerCase();
                        if (keyPath.equals(targetPath) || 
                            keyPath.equals(targetPath.replace("_resistant", "_resistance")) ||
                            keyPath.equals(targetPath.replace("_resistance", "_resistant")) ||
                            keyPath.equals("is_" + targetPath) ||
                            targetPath.endsWith(keyPath) ||
                            keyPath.endsWith(targetPath)) {
                            
                            componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(key)
                                    .map(Holder::value)
                                    .orElse(null);
                            if (componentType != null) {
                                LOGGER.info("Found component '{}' via fallback as '{}'", componentIdString, key);
                                break;
                            }
                        }
                    }
                }

                if (componentType == null) {
                    LOGGER.error("Unknown data component '{}' for item {}", componentIdString, item);
                    
                    // Log all components once if this happens, to help identify the correct names
                    if (BuiltInRegistries.DATA_COMPONENT_TYPE.keySet().size() > 0) {
                        List<String> allKeys = BuiltInRegistries.DATA_COMPONENT_TYPE.keySet().stream()
                                .map(ResourceLocation::toString)
                                .sorted()
                                .toList();
                        LOGGER.info("Available DataComponentTypes: {}", String.join(", ", allKeys));
                    }
                    continue;
                }

                final DataComponentType<?> finalComponentType = componentType;
                var codec = finalComponentType.codecOrThrow();
                JsonElement componentJsonElement = GSON.toJsonTree(jsonValue);
                var parseResult = codec.parse(JsonOps.INSTANCE, componentJsonElement);
                
                parseResult.result().ifPresent(parsedValue -> {
                    @SuppressWarnings("unchecked")
                    DataComponentType<Object> typedComponentType = (DataComponentType<Object>) finalComponentType;
                    newMapBuilder.set(typedComponentType, parsedValue);
                    LOGGER.info("Applied data component {} to {}", componentIdString, item);
                });
                
                if (parseResult.error().isPresent()) {
                    LOGGER.error("Failed to parse component '{}' for item {}: {}", componentIdString, item, parseResult.error().get().message());
                }
            } catch (Exception e) {
                LOGGER.error("Error processing component '{}' for item {}: {}", componentIdString, item, e.getMessage());
            }
        }
    }

    public static Holder<Attribute> resolveAttribute(String idString) {
        ResourceLocation id = ResourceLocation.parse(idString);
        var holder = BuiltInRegistries.ATTRIBUTE.get(id);
        
        if (holder.isEmpty()) {
            String targetPath = id.getPath().toLowerCase();
            if (targetPath.startsWith("generic.")) {
                targetPath = targetPath.substring(8);
            }
            
            // Search registry by path
            for (ResourceLocation key : BuiltInRegistries.ATTRIBUTE.keySet()) {
                String keyPath = key.getPath().toLowerCase();
                if (keyPath.startsWith("generic.")) {
                    keyPath = keyPath.substring(8);
                }
                
                if (keyPath.equals(targetPath)) {
                    holder = BuiltInRegistries.ATTRIBUTE.get(key);
                    if (holder.isPresent()) {
                        LOGGER.info("Found attribute '{}' via fallback as '{}'", idString, key);
                        break;
                    }
                }
            }
        }
        
        if (holder.isEmpty()) {
            LOGGER.error("Unknown attribute '{}'. Available attributes: {}", idString, 
                    BuiltInRegistries.ATTRIBUTE.keySet().stream().map(ResourceLocation::toString).toList());
        }
        
        return holder.orElse(null);
    }

    private static double calculateInternalValue(Item item, Holder<Attribute> attribute, double targetValue) {
        double baseValue = 0;
        if (attribute.is(Attributes.ATTACK_DAMAGE)) {
            baseValue = 1.0;
        } else if (attribute.is(Attributes.ATTACK_SPEED)) {
            baseValue = 4.0;
        }
        return targetValue - baseValue;
    }

    private static AttributeModifier.Operation determineOperation(Holder<Attribute> attribute) {
        if (attribute.is(Attributes.MOVEMENT_SPEED)) {
            return AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        }
        return AttributeModifier.Operation.ADD_VALUE;
    }

    private static EquipmentSlotGroup determineEquipmentSlot(Item item) {
        // Use string-based detection as a robust fallback since ArmorItem class location varies between versions/mappings
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.contains("helmet")) return EquipmentSlotGroup.HEAD;
        if (path.contains("chestplate")) return EquipmentSlotGroup.CHEST;
        if (path.contains("leggings")) return EquipmentSlotGroup.LEGS;
        if (path.contains("boots")) return EquipmentSlotGroup.FEET;
        if (path.contains("horse_armor")) return EquipmentSlotGroup.BODY;
        
        return EquipmentSlotGroup.MAINHAND;
    }

    /**
     * Generates a unique ResourceLocation ID for an attribute modifier.
     */
    private static ResourceLocation generateModifierId(Item item, Holder<Attribute> attribute) {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String attrPath = attribute.unwrapKey()
                .map(key -> key.location().toString().replace(':', '/'))
                .orElse("unknown");

        String path = "modifier/" + itemKey.toString().replace(':', '/') + "/" + attrPath;
        return ResourceLocation.fromNamespaceAndPath("stattweaks", path);
    }
}
