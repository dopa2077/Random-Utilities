package com.dopa.randomutilities.fishnet.config;

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

/** Fishnet options from {@code config/dopas_random_utilities/blocks/fishnet.json}. */
public final class FishnetConfig {
    private static final String CONFIG_RELATIVE = "dopas_random_utilities/blocks/fishnet.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/blocks/fishnet.json";

    private static boolean preventRareLoot = false;

    static {
        loadDefaultsFromJar();
    }

    private FishnetConfig() {}

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
                    "Failed to load fishnet config from {}, using defaults",
                    configFile,
                    exception
            );
            loadDefaultsFromJar();
        }
    }

    public static void reload() {
        load();
    }

    /**
     * When true, the fishnet never rolls fishing treasure (open water and Fortune Mesh ignored).
     * Default false.
     */
    public static boolean preventRareLoot() {
        return preventRareLoot;
    }

    private static void loadDefaultsFromJar() {
        try (InputStream input = FishnetConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled fishnet config defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = FishnetConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote fishnet config at {}", configFile);
        }
    }

    private static void applyBuiltInDefaults() {
        preventRareLoot = false;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root.has("prevent_rare_loot")) {
            preventRareLoot = root.get("prevent_rare_loot").getAsBoolean();
        }
    }
}
