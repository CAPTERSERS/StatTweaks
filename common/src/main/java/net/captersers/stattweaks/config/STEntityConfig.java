package net.captersers.stattweaks.config;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a single entity type, containing only attribute modifiers.
 *
 * Entity configurations are simpler than item configurations because entities
 * don't have data components; only attribute modifiers matter.
 *
 * Example JSON:
 * <pre>
 * {
 *   "attributes": {
 *     "minecraft:generic.max_health": 30.0,
 *     "minecraft:generic.movement_speed": 0.25,
 *     "minecraft:generic.attack_damage": 4.0
 *   }
 * }
 * </pre>
 */
public class STEntityConfig {

    /**
     * Attribute modifiers for this entity type.
     * Keys: Attribute IDs (e.g., "minecraft:generic.max_health", "generic.attack_damage")
     * Values: Base values to set for all entities of this type
     *
     * Important: These are BASE VALUES, not modifiers. They will override the default
     * values defined in the entity's attribute supplier.
     */
    @SerializedName("attributes")
    public Map<String, Double> attributes = new HashMap<>();

    /**
     * Checks if this config has any modifications to apply.
     *
     * @return true if there are attributes
     */
    public boolean hasModifications() {
        return attributes != null && !attributes.isEmpty();
    }
}

