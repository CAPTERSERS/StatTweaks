package net.captersers.stattweaks;

import net.captersers.stattweaks.processor.EntityProcessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles application of entity attribute modifications.
 */
public class STEntityAttributeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("STEntityAttributeHandler");

    private STEntityAttributeHandler() {
        // Utility class
    }

    public static void init() {
        LOGGER.debug("STEntityAttributeHandler initialized");
    }

    /**
     * Applies attribute modifications to a living entity based on its type.
     *
     * @param entity The entity to potentially modify
     */
    public static void applyToEntity(LivingEntity entity) {
        if (entity == null) return;

        EntityType<?> entityType = entity.getType();
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);

        if (entityTypeId == null) return;

        Map<String, Double> modifications = EntityProcessor.getModificationsForEntityType(entityTypeId);

        if (modifications.isEmpty()) return;

        for (Map.Entry<String, Double> entry : modifications.entrySet()) {
            String attributeIdString = entry.getKey();
            Double baseValue = entry.getValue();

            if (baseValue == null) continue;

            try {
                ResourceLocation attributeId = ResourceLocation.parse(attributeIdString);
                var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);

                if (attributeHolder.isPresent()) {
                    AttributeInstance attributeInstance = entity.getAttribute(attributeHolder.get());

                    if (attributeInstance != null) {
                        double oldBase = attributeInstance.getBaseValue();
                        
                        // If the value is already what we want, don't do anything
                        if (oldBase == baseValue) continue;

                        attributeInstance.setBaseValue(baseValue);
                        
                        // Special handling for Max Health to keep current health consistent
                        if (attributeHolder.get().is(Attributes.MAX_HEALTH) && oldBase > 0) {
                            float healthRatio = entity.getHealth() / (float) oldBase;
                            float newHealth = (float) (baseValue * healthRatio);
                            entity.setHealth(Math.min(baseValue.floatValue(), newHealth));
                        }
                        
                        LOGGER.debug("Applied attribute '{}' with base value {} to entity {}",
                                attributeIdString, baseValue, entityTypeId);
                    }
                } else {
                    LOGGER.warn("Unknown attribute identifier '{}' for entity type {}", attributeIdString, entityTypeId);
                }
            } catch (Exception e) {
                LOGGER.error("Error applying attribute '{}' to entity {}: {}", attributeIdString, entityTypeId, e.getMessage());
            }
        }
    }
}


