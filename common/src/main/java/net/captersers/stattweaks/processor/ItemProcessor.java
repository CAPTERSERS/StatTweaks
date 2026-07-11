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
import net.minecraft.world.item.ArmorItem;
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
                injectAttributeModifiers(item, itemConfig.attributes, newMapBuilder);
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
    private static void injectAttributeModifiers(Item item, Map<String, Double> attributes, DataComponentMap.Builder newMapBuilder) {
        ItemAttributeModifiers originalModifiers = newMapBuilder.build().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (originalModifiers == null) {
            originalModifiers = ItemAttributeModifiers.EMPTY;
        }

        ItemAttributeModifiers.Builder attrBuilder = ItemAttributeModifiers.builder();
        Map<Holder<Attribute>, Double> standardAttributes = new HashMap<>();
        
        // Handle special attributes first
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();

            if ("stattweaks:durability".equals(key)) {
                newMapBuilder.set(DataComponents.MAX_DAMAGE, (int) value);
            } else if ("stattweaks:efficiency".equals(key)) {
                applyToolEfficiency(item, value, newMapBuilder);
            } else {
                Holder<Attribute> attribute = resolveAttribute(key);
                if (attribute != null) {
                    standardAttributes.put(attribute, value);
                }
            }
        }

        // Determine equipment slot based on item type
        EquipmentSlotGroup slotGroup = determineEquipmentSlot(item);

        // Retain modifiers that are NOT being overridden (both vanilla and previous tags)
        for (ItemAttributeModifiers.Entry entry : originalModifiers.modifiers()) {
            if (!standardAttributes.containsKey(entry.attribute())) {
                attrBuilder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // Add new/overridden modifiers
        for (Map.Entry<Holder<Attribute>, Double> entry : standardAttributes.entrySet()) {
            Holder<Attribute> attribute = entry.getKey();
            double targetValue = entry.getValue();
            
            double internalValue = calculateInternalValue(item, attribute, targetValue);
            AttributeModifier.Operation operation = determineOperation(attribute);

            // Generate a unique ID for this modifier to avoid collisions with other equipped items
            ResourceLocation modifierId = generateModifierId(item, attribute);

            attrBuilder.add(attribute,
                    new AttributeModifier(modifierId, internalValue, operation),
                    slotGroup);
        }

        // Set the final attribute modifiers component, with tooltip setting based on config
        boolean showInTooltip = !"base".equals(net.captersers.stattweaks.manager.STBalanceManager.getTooltipMode());
        newMapBuilder.set(DataComponents.ATTRIBUTE_MODIFIERS, attrBuilder.build().withTooltip(showInTooltip));
    }

    /**
     * Specialized logic for tool efficiency.
     */
    private static void applyToolEfficiency(Item item, double value, DataComponentMap.Builder newMapBuilder) {
        Tool tool = newMapBuilder.build().get(DataComponents.TOOL);
        if (tool != null) {
            List<Tool.Rule> newRules = new ArrayList<>();
            for (Tool.Rule rule : tool.rules()) {
                newRules.add(new Tool.Rule(rule.blocks(), Optional.of((float) value), rule.correctForDrops()));
            }
            newMapBuilder.set(DataComponents.TOOL, new Tool(newRules, (float) value, tool.damagePerBlock()));
            LOGGER.debug("Updated tool efficiency rules for {}", item);
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
                DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(componentId);

                if (componentType == null) {
                    LOGGER.error("Unknown data component '{}' for item {}", componentIdString, item);
                    continue;
                }

                var codec = componentType.codecOrThrow();
                JsonElement componentJsonElement = GSON.toJsonTree(jsonValue);
                var parseResult = codec.parse(JsonOps.INSTANCE, componentJsonElement);
                
                parseResult.result().ifPresent(parsedValue -> {
                    @SuppressWarnings("unchecked")
                    DataComponentType<Object> typedComponentType = (DataComponentType<Object>) componentType;
                    newMapBuilder.set(typedComponentType, parsedValue);
                    LOGGER.debug("Applied data component {} to {}", componentIdString, item);
                });
                
                if (parseResult.error().isPresent()) {
                    LOGGER.error("Failed to parse component '{}' for item {}: {}", componentIdString, item, parseResult.error().get().message());
                }
            } catch (Exception e) {
                LOGGER.error("Error processing component '{}' for item {}: {}", componentIdString, item, e.getMessage());
            }
        }
    }

    private static Holder<Attribute> resolveAttribute(String id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id)).orElse(null);
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
        if (item instanceof ArmorItem armor) {
            return switch (armor.getType()) {
                case HELMET -> EquipmentSlotGroup.HEAD;
                case CHESTPLATE -> EquipmentSlotGroup.CHEST;
                case LEGGINGS -> EquipmentSlotGroup.LEGS;
                case BOOTS -> EquipmentSlotGroup.FEET;
                case BODY -> EquipmentSlotGroup.BODY;
            };
        }
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
