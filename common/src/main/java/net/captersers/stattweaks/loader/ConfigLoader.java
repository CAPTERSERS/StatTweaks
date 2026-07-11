package net.captersers.stattweaks.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.captersers.stattweaks.config.STConfig;
import net.captersers.stattweaks.config.STEntityConfig;
import net.captersers.stattweaks.config.STItemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for loading and deserializing the StatTweaks configuration from JSON.
 *
 * This class handles:
 * - Reading {@code CPT_StatTweaks_Config.json} from the config directory
 * - Deserializing JSON to {@link STConfig} using Gson
 * - Creating default configuration if file doesn't exist
 * - Robust error handling and logging
 */
public class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigLoader");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final String CONFIG_FILENAME = "CPT_StatTweaks_Config.json";

    private ConfigLoader() {
        // Utility class, no instantiation
    }

    /**
     * Loads the StatTweaks configuration from the config directory.
     *
     * @param configDir The config directory path
     * @return A {@link STConfig} object, or null if loading fails
     */
    public static STConfig loadConfiguration(Path configDir) {
        Path configFile = configDir.resolve(CONFIG_FILENAME);

        // Create default config if missing
        if (!Files.exists(configFile)) {
            createDefaultConfiguration(configFile);
        }

        return parseConfigFile(configFile);
    }

    /**
     * Parses a configuration string and returns a STConfig object.
     *
     * @param json The JSON string to parse
     * @return A {@link STConfig} object, or null if parsing fails
     */
    public static STConfig parseConfigString(String json) {
        try {
            STConfig config = GSON.fromJson(json, STConfig.class);

            if (config == null) {
                LOGGER.error("Configuration string is empty or invalid");
                return null;
            }

            // Ensure maps are initialized
            if (config.items == null) config.items = new java.util.HashMap<>();
            if (config.entities == null) config.entities = new java.util.HashMap<>();

            return config;
        } catch (Exception e) {
            LOGGER.error("Failed to parse configuration string: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses the configuration file and returns a STConfig object.
     *
     * @param configFile The path to the configuration file
     * @return A {@link STConfig} object, or null if parsing fails
     */
    private static STConfig parseConfigFile(Path configFile) {
        try (Reader reader = Files.newBufferedReader(configFile)) {
            STConfig config = GSON.fromJson(reader, STConfig.class);

            if (config == null) {
                LOGGER.error("Configuration file is empty or invalid: {}", configFile);
                return null;
            }

            // Ensure maps are initialized (handle null from Gson if @SerializedName annotations are missing)
            if (config.items == null) {
                config.items = new HashMap<>();
            }
            if (config.entities == null) {
                config.entities = new HashMap<>();
            }

            LOGGER.info("Successfully loaded configuration with {} item and {} entity entries", 
                    config.items.size(), config.entities.size());
            return config;

        } catch (Exception e) {
            LOGGER.error("Failed to parse configuration file {}: {}", configFile, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates a default configuration file if it doesn't exist.
     *
     * @param configFile The path where the default config should be created
     */
    private static void createDefaultConfiguration(Path configFile) {
        try {
            Files.createDirectories(configFile.getParent());

            STConfig defaultConfig = createDefaultConfig();
            String json = GSON.toJson(defaultConfig);

            try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
                writer.write(json);
            }

            LOGGER.info("Created default configuration file at: {}", configFile);

        } catch (Exception e) {
            LOGGER.error("Failed to create default configuration file: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates a default STConfig with example entries.
     *
     * @return A new {@link STConfig} with example item and entity configurations
     */
    private static STConfig createDefaultConfig() {
        STConfig defaultConfig = new STConfig();
        defaultConfig.tooltipMode = "relative";
        defaultConfig.items = new HashMap<>();
        defaultConfig.entities = new HashMap<>();

        // We leave it empty as per user request, so players can fill it themselves.
        // A minimal example could be added here if needed, but "empty" was specified.

        return defaultConfig;
    }

    /**
     * Validates a single item configuration for safety.
     * Logs warnings for suspicious values but doesn't fail.
     *
     * @param itemId The item ID being validated
     * @param config The configuration to validate
     * @return true if the config passes validation, false if it should be skipped
     */
    public static boolean validateItemConfig(String itemId, STItemConfig config) {
        if (config == null) {
            LOGGER.warn("Item configuration for {} is null, skipping", itemId);
            return false;
        }

        if (!config.hasModifications()) {
            LOGGER.warn("Item configuration for {} has no attributes, components, or conditions", itemId);
            return false;
        }

        // Additional validation can be added here in the future
        return true;
    }
}
