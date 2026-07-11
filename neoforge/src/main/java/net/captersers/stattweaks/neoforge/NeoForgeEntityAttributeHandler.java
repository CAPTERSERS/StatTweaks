package net.captersers.stattweaks.neoforge;

import net.captersers.stattweaks.STEntityAttributeHandler;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge-specific handler for applying entity attribute modifications.
 */
public class NeoForgeEntityAttributeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("NeoForgeEntityAttributeHandler");

    private NeoForgeEntityAttributeHandler() {
        // Utility class
    }

    public static void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(NeoForgeEntityAttributeHandler.class);
        LOGGER.debug("NeoForgeEntityAttributeHandler initialized");
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            STEntityAttributeHandler.applyToEntity(livingEntity);
        }
    }
}

