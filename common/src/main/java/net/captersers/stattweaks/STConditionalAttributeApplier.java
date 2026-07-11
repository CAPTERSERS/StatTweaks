package net.captersers.stattweaks;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.captersers.stattweaks.config.STConditionalModifier;
import net.captersers.stattweaks.manager.STBalanceManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class STConditionalAttributeApplier {

    private static final Logger LOGGER = LoggerFactory.getLogger("ConditionalStats");
    // NOTE: switched to ResourceLocation-based ids for attribute modifiers (mapping-friendly)

    // Map to store currently applied conditional modifiers per player
    // Player UUID -> Map<Modifier Name (e.g., "nether_boost"), Map<Attribute Holder, AttributeModifier>>
    private static final Map<UUID, Map<String, Map<Holder<Attribute>, AttributeModifier>>> APPLIED_MODIFIERS = new HashMap<>();

    // Map to store the last known equipped items for each player to detect changes
    // Player UUID -> Map<EquipmentSlot, Item>
    private static final Map<UUID, Map<EquipmentSlot, Item>> LAST_EQUIPPED_ITEMS = new HashMap<>();

    public static void init() {
        // Register server tick event to periodically check conditions
        TickEvent.SERVER_POST.register(STConditionalAttributeApplier::onServerTick);

        // Register player join/quit to manage APPLIED_MODIFIERS and LAST_EQUIPPED_ITEMS
        PlayerEvent.PLAYER_JOIN.register(player -> {
            APPLIED_MODIFIERS.put(player.getUUID(), new HashMap<>());
            LAST_EQUIPPED_ITEMS.put(player.getUUID(), new HashMap<>());
            // Initial evaluation when player joins
            evaluatePlayerConditions(player);
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            // Clean up any remaining modifiers and clear caches
            removePlayerConditionalModifiers(player);
            APPLIED_MODIFIERS.remove(player.getUUID());
            LAST_EQUIPPED_ITEMS.remove(player.getUUID());
        });
    }

    private static void onServerTick(net.minecraft.server.MinecraftServer server) {
        // Only run checks every 20 ticks (1 second) to reduce load
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        for (Player player : server.getPlayerList().getPlayers()) {
            evaluatePlayerConditions(player);
        }
    }

    private static void evaluatePlayerConditions(Player player) {
        Map<String, Map<Holder<Attribute>, AttributeModifier>> playerAppliedModifiers = APPLIED_MODIFIERS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        Map<EquipmentSlot, Item> playerLastEquipped = LAST_EQUIPPED_ITEMS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());

        boolean equipmentChanged = false;
        Map<EquipmentSlot, Item> currentEquippedItems = new HashMap<>();

        // Check equipped items and detect changes
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // Explicitly check slots we care about to avoid relying on potentially-removed helper types
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND
                    || slot == EquipmentSlot.FEET || slot == EquipmentSlot.LEGS
                    || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.HEAD) {
                ItemStack equippedStack = player.getItemBySlot(slot);
                Item equippedItem = equippedStack.getItem();
                currentEquippedItems.put(slot, equippedItem);

                if (!playerLastEquipped.containsKey(slot) || playerLastEquipped.get(slot) != equippedItem) {
                    equipmentChanged = true;
                }
            }
        }

        // If equipment changed or it's the first check for the player, re-evaluate all
        if (equipmentChanged || playerLastEquipped.isEmpty()) {
            // Remove all previously applied conditional modifiers for this player
            removePlayerConditionalModifiers(player);
            playerAppliedModifiers.clear(); // Clear our tracking map

            // Update last equipped items
            playerLastEquipped.clear();
            playerLastEquipped.putAll(currentEquippedItems);
        }

        // Now, re-evaluate all conditions for all equipped items and apply/remove as needed
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // Explicit slot filtering, see above comment
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND
                    || slot == EquipmentSlot.FEET || slot == EquipmentSlot.LEGS
                    || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.HEAD) {
                Item equippedItem = currentEquippedItems.get(slot);

                Map<String, STConditionalModifier> itemConditionalModifiers = STBalanceManager.getConditionalModifiers().get(equippedItem);
                if (itemConditionalModifiers != null && !itemConditionalModifiers.isEmpty()) {
                    for (Map.Entry<String, STConditionalModifier> entry : itemConditionalModifiers.entrySet()) {
                        String modifierName = entry.getKey();
                        STConditionalModifier conditionalModifier = entry.getValue();

                        boolean conditionMet = checkConditions(player, conditionalModifier.conditions);
                        boolean modifierCurrentlyApplied = playerAppliedModifiers.containsKey(modifierName);

                        if (conditionMet && !modifierCurrentlyApplied) {
                            applyConditionalModifiers(player, modifierName, conditionalModifier.attributes_modifier, slot);
                        } else if (!conditionMet && modifierCurrentlyApplied) {
                            removeConditionalModifiers(player, modifierName);
                        }
                    }
                }
            }
        }
    }

    private static boolean checkConditions(Player player, Map<String, String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions means always true
        }

        for (Map.Entry<String, String> conditionEntry : conditions.entrySet()) {
            String conditionType = conditionEntry.getKey();
            String expectedValue = conditionEntry.getValue();

            switch (conditionType) {
                case "dimension":
                    ResourceLocation currentDimension = player.level().dimension().location();
                    ResourceLocation expectedDimension = ResourceLocation.parse(expectedValue);
                    if (!currentDimension.equals(expectedDimension)) {
                        return false; // Condition not met
                    }
                    break;
                // Add more condition types here (e.g., "biome", "time", "weather", etc.)
                default:
                    LOGGER.warn("Unknown condition type '{}' for player {}", conditionType, player.getName().getString());
                    return false; // Unknown condition, treat as not met
            }
        }
        return true; // All conditions met
    }

    private static void applyConditionalModifiers(Player player, String modifierName, Map<String, Double> attributesToApply, EquipmentSlot slot) {
        Map<String, Map<Holder<Attribute>, AttributeModifier>> playerAppliedModifiers = APPLIED_MODIFIERS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        Map<Holder<Attribute>, AttributeModifier> modifiersForThisCondition = playerAppliedModifiers.computeIfAbsent(modifierName, k -> new HashMap<>());

        for (Map.Entry<String, Double> attrEntry : attributesToApply.entrySet()) {
            String attributeIdString = attrEntry.getKey();
            Double value = attrEntry.getValue();

            try {
                ResourceLocation attributeId = ResourceLocation.parse(attributeIdString);
                Holder<Attribute> attributeHolder = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);

                if (attributeHolder != null) {
                    // Pass the Holder<Attribute> directly to getAttribute (mapping expects a Holder)
                    AttributeInstance attributeInstance = player.getAttribute(attributeHolder);
                    if (attributeInstance != null) {
                        // Create a ResourceLocation-based id for the modifier (mapping-friendly API)
                        String safePath = ("conditional_" + modifierName + "_" + attributeIdString)
                                .replace(':', '_').replace('.', '_');
                        ResourceLocation modifierId = ResourceLocation.parse("stattweaks:" + safePath);

                        AttributeModifier modifier = new AttributeModifier(modifierId, value, AttributeModifier.Operation.ADD_VALUE);

                        // Ensure no previous instance of this modifier remains (avoid stacking)
                        try {
                            attributeInstance.removeModifier(modifier);
                        } catch (Exception ignored) {
                            // Some mappings may not support remove by object; ignore and proceed to addTransient
                        }
                        attributeInstance.addTransientModifier(modifier);
                        modifiersForThisCondition.put(attributeHolder, modifier); // Store the modifier for later removal
                        LOGGER.debug("Applied conditional attribute {} with value {} to player {} for item in slot {} (Condition: {})", attributeIdString, value, player.getName().getString(), slot.toString(), modifierName);
                    } else {
                        LOGGER.warn("Player does not have attribute '{}'. Cannot apply conditional modifier.", attributeIdString);
                    }
                } else {
                    LOGGER.warn("Unknown attribute identifier '{}'. Cannot apply conditional modifier.", attributeIdString);
                }
            } catch (Exception e) {
                LOGGER.error("Error applying conditional attribute '{}' for player {}: {}", attributeIdString, player.getName().getString(), e.getMessage());
            }
        }
    }

    private static void removeConditionalModifiers(Player player, String modifierName) {
        Map<String, Map<Holder<Attribute>, AttributeModifier>> playerAppliedModifiers = APPLIED_MODIFIERS.get(player.getUUID());
        if (playerAppliedModifiers == null) {
            return;
        }
        Map<Holder<Attribute>, AttributeModifier> modifiersForThisCondition = playerAppliedModifiers.get(modifierName);
        if (modifiersForThisCondition == null || modifiersForThisCondition.isEmpty()) {
            return; // Modifier not applied or already removed
        }

        for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : modifiersForThisCondition.entrySet()) {
            Holder<Attribute> attributeHolder = entry.getKey();
            AttributeModifier modifier = entry.getValue();

            // Pass the Holder<Attribute> directly to getAttribute (mapping expects a Holder)
            AttributeInstance attributeInstance = player.getAttribute(attributeHolder);
            		if (attributeInstance != null) {
            			try {
            				attributeInstance.removeModifier(modifier);
            				LOGGER.debug("Removed conditional attribute {} from player {} (Condition: {})", attributeHolder.unwrapKey().map(k -> k.location().toString()).orElse("N/A"), player.getName().getString(), modifierName);
            			} catch (Exception ignored) {
            				// Fallback: if remove by object not supported, attempt to remove by id
            				try {
            					attributeInstance.removeModifier(modifier.id());
            					LOGGER.debug("Removed conditional attribute {} from player {} (Condition: {}) [by id]", attributeHolder.unwrapKey().map(k -> k.location().toString()).orElse("N/A"), player.getName().getString(), modifierName);
            				} catch (Exception ignored2) {
            					// Give up silently; it may already be gone
            				}
            			}
            		}
        }
        playerAppliedModifiers.remove(modifierName); // Remove the entry for this condition
    }

    public static void removePlayerConditionalModifiers(Player player) {
        Map<String, Map<Holder<Attribute>, AttributeModifier>> playerAppliedModifiers = APPLIED_MODIFIERS.get(player.getUUID());
        if (playerAppliedModifiers != null) {
            for (String modifierName : new HashMap<>(playerAppliedModifiers).keySet()) { // Iterate over a copy to avoid ConcurrentModificationException
                removeConditionalModifiers(player, modifierName);
            }
            playerAppliedModifiers.clear();
        }
    }
}
