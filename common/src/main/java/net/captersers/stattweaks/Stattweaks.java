package net.captersers.stattweaks;

import net.captersers.stattweaks.manager.STBalanceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the StatTweaks mod.
 *
 * Coordinates initialization of all subsystems:
 * - STAttributes: Custom attribute registration
 * - STBalanceManager: Item and entity configuration loading and application
 * - STConditionalAttributeApplier: Conditional modifier management
 * - STEntityAttributeHandler: Entity attribute modification initialization
 */
public final class Stattweaks {

    public static final String MOD_ID = "stattweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Stattweaks() {
        // Static utility class, no instantiation
    }

    /**
     * Initializes the StatTweaks mod.
     * Called by both Fabric and NeoForge entry points.
     */
    public static void init() {
        LOGGER.info("Initializing StatTweaks mod...");

        STAttributes.init();                    // Register custom attributes
        STEntityAttributeHandler.init();         // Setup entity attribute modification handler
        STBalanceManager.init();                 // Load and apply item/entity configurations
        STConditionalAttributeApplier.init();    // Set up conditional modifiers
    }
}

