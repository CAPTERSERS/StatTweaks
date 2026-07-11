package net.captersers.stattweaks.config;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a single item, including attributes, data components, and conditional modifiers.
 *
 * Example JSON:
 * <pre>
 * {
 *   "attributes": {
 *     "generic.attack_damage": 8.5,
 *     "generic.attack_speed": 1.8
 *   },
 *   "components": {
 *     "minecraft:fire_resistant": {},
 *     "minecraft:rarity": "epic"
 *   },
 *   "conditions": {
 *     "nether_boost": { ... }
 *   }
 * }
 * </pre>
 */
public class STItemConfig {

    /**
     * Attribute modifiers for this item.
     * Keys: Attribute IDs (e.g., "generic.attack_damage", "minecraft:generic.attack_speed")
     * Values: Desired double values to apply
     */
    @SerializedName("attributes")
    public Map<String, Double> attributes = new HashMap<>();

    /**
     * Data components for this item.
     * Keys: Component IDs (e.g., "minecraft:fire_resistant", "minecraft:rarity")
     * Values: Component values (can be empty for marker components, primitives, or objects)
     */
    @SerializedName("components")
    public Map<String, Object> components = new HashMap<>();

    /**
     * Conditional attribute modifiers.
     * Keys: Descriptive names (e.g., "nether_boost", "underwater_bonus")
     * Values: Condition + modifier pairs
     */
    @SerializedName("conditions")
    public Map<String, STConditionalModifier> conditions = new HashMap<>();

    /**
     * Checks if this config has any modifications to apply.
     *
     * @return true if there are attributes, components, or conditions
     */
    public boolean hasModifications() {
        return (attributes != null && !attributes.isEmpty()) || 
               (components != null && !components.isEmpty()) || 
               (conditions != null && !conditions.isEmpty());
    }
}
