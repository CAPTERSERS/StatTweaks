package net.captersers.stattweaks.config;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

/**
 * Root configuration POJO for StatTweaks.
 *
 * This class represents the entire configuration structure loaded from
 * {@code CPT_StatTweaks_Config.json}. It maps item identifiers and entity types
 * (ResourceLocation strings) to their respective configurations.
 *
 * Example JSON structure:
 * <pre>
 * {
 *   "items": {
 *     "minecraft:diamond_sword": {
 *       "attributes": { ... },
 *       "components": { ... },
 *       "conditions": { ... }
 *     }
 *   },
 *   "entities": {
 *     "minecraft:zombie": {
 *       "attributes": { ... }
 *     }
 *   }
 * }
 * </pre>
 */
public class STConfig {

    /**
     * Tooltip display mode for item attributes.
     * "relative" (default): Shows bonuses like +5 Damage.
     * "base": Shows total values like 6 Damage.
     */
    @SerializedName("tooltip_mode")
    public String tooltipMode = "relative";



    /**
     * Map of item identifiers to their configurations.
     * Keys can be:
     * - Item IDs: "minecraft:diamond_sword"
     * - Tags: "#minecraft:axes"
     */
    @SerializedName("items")
    public Map<String, STItemConfig> items = new HashMap<>();

    /**
     * Map of entity type identifiers to their configurations.
     * Keys are entity type IDs: "minecraft:zombie", "minecraft:creeper", etc.
     */
    @SerializedName("entities")
    public Map<String, STEntityConfig> entities = new HashMap<>();

    /**
     * Validates that the configuration has items or entities.
     *
     * @return true if the config has items or entities, false otherwise
     */
    public boolean isValid() {
        return (items != null && !items.isEmpty()) || (entities != null && !entities.isEmpty());
    }
}
