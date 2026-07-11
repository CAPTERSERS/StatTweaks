package net.captersers.stattweaks.fabric;

import net.captersers.stattweaks.STEntityAttributeHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric-specific handler for applying entity attribute modifications.
 */
public class FabricEntityAttributeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("FabricEntityAttributeHandler");

    private FabricEntityAttributeHandler() {
        // Utility class
    }

    public static void init() {
        // Listen for entities being loaded into the world
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof LivingEntity livingEntity) {
                STEntityAttributeHandler.applyToEntity(livingEntity);
            }
        });

        LOGGER.debug("FabricEntityAttributeHandler initialized");
    }
}


