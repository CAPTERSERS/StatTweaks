package net.captersers.stattweaks.config;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a set of attribute modifiers that are applied conditionally.
 *
 * Example JSON:
 * <pre>
 * {
 *   "conditions": {
 *     "dimension": "minecraft:the_nether"
 *   },
 *   "attributes_modifier": {
 *     "generic.armor": 2.0,
 *     "generic.movement_speed": -0.05
 *   }
 * }
 * </pre>
 */
public class STConditionalModifier {

    /**
     * Conditions that must be met to apply the modifier.
     * Currently supported condition types:
     * - "dimension": ResourceLocation of the dimension (e.g., "minecraft:the_nether")
     * More can be added in the future (biome, weather, time, etc.)
     */
    @SerializedName("conditions")
    public Map<String, String> conditions = new HashMap<>();

    /**
     * Attribute modifiers to apply when conditions are met.
     * Keys: Attribute IDs (e.g., "generic.armor")
     * Values: Modifier values (positive or negative)
     */
    @SerializedName("attributes_modifier")
    public Map<String, Double> attributes_modifier = new HashMap<>();

    /**
     * Validates that both conditions and modifiers are set.
     *
     * @return true if both maps have entries
     */
    public boolean isValid() {
        return !conditions.isEmpty() && !attributes_modifier.isEmpty();
    }
}
