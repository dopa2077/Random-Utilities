package com.dopa.randomutilities.itemcollector.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Item collector options from {@code config/dopas_random_utilities/blocks/item_collector.json}. */
public final class ItemCollectorConfig {
    private static final String CONFIG_RELATIVE = "dopas_random_utilities/blocks/item_collector.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/blocks/item_collector.json";

    private static boolean lineOfSightEnabled = true;

    static {
        loadDefaultsFromJar();
    }

    private ItemCollectorConfig() {}

    public static void load() {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_RELATIVE);
        try {
            Files.createDirectories(configFile.getParent());
            if (Files.notExists(configFile)) {
                copyDefaultConfig(configFile);
            }
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                applyJson(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error(
                    "Failed to load item collector config from {}, using defaults",
                    configFile,
                    exception
            );
            loadDefaultsFromJar();
        }
    }

    public static void reload() {
        load();
    }

    /** When false, LOS checks are skipped and the UI toggle is greyed out. Default true. */
    public static boolean lineOfSightEnabled() {
        return lineOfSightEnabled;
    }

    private static void loadDefaultsFromJar() {
        try (InputStream input = ItemCollectorConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled item collector config defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = ItemCollectorConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote item collector config at {}", configFile);
        }
    }

    private static void applyBuiltInDefaults() {
        lineOfSightEnabled = true;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root.has("line_of_sight_enabled")) {
            lineOfSightEnabled = root.get("line_of_sight_enabled").getAsBoolean();
        }
    }
}
