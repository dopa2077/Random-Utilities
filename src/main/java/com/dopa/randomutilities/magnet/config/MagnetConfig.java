package com.dopa.randomutilities.magnet.config;

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

/** Magnet range, speed, and upgrade caps from {@code config/dopas_random_utilities/items/magnet.json}. */
public final class MagnetConfig {
    private static final String CONFIG_RELATIVE = "dopas_random_utilities/items/magnet.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/magnet.json";

    private static int baseRange = 4;
    private static int maxRange = 8;
    private static int maxRangeUpgrades = 16;
    private static int maxOverclock = 11;
    private static int baseTicks = 10;
    private static double pullSpeed = 0.35;
    private static int baseEntities = 8;
    private static int maxEntities = 32;

    static {
        loadDefaultsFromJar();
    }

    private MagnetConfig() {}

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
            dOPasRandomUtilities.LOGGER.error("Failed to load magnet config from {}, using defaults", configFile, exception);
            loadDefaultsFromJar();
        }
    }

    public static void reload() {
        load();
    }

    public static int baseRange() {
        return Math.max(0, baseRange);
    }

    public static int maxRange() {
        return Math.max(0, maxRange);
    }

    public static int maxRangeUpgrades() {
        return Math.max(0, maxRangeUpgrades);
    }

    public static int maxOverclock() {
        return Math.max(0, maxOverclock);
    }

    public static int baseTicks() {
        return Math.max(1, baseTicks);
    }

    public static double pullSpeed() {
        return Math.max(0.01, pullSpeed);
    }

    public static int baseEntities() {
        return Math.max(1, baseEntities);
    }

    public static int maxEntities() {
        return Math.max(baseEntities(), maxEntities);
    }

    private static void loadDefaultsFromJar() {
        try (InputStream input = MagnetConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled magnet config defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = MagnetConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote magnet config at {}", configFile);
        }
    }

    private static void applyBuiltInDefaults() {
        baseRange = 4;
        maxRange = 8;
        maxRangeUpgrades = 16;
        maxOverclock = 11;
        baseTicks = 10;
        pullSpeed = 0.35;
        baseEntities = 8;
        maxEntities = 32;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root == null) {
            return;
        }
        baseRange = intOr(root, "base_range", baseRange);
        maxRange = intOr(root, "max_range", maxRange);
        maxRangeUpgrades = intOr(root, "max_range_upgrades", maxRangeUpgrades);
        maxOverclock = intOr(root, "max_overclock", maxOverclock);
        baseTicks = Math.max(1, intOr(root, "base_ticks", baseTicks));
        pullSpeed = doubleOr(root, "pull_speed", pullSpeed);
        baseEntities = Math.max(1, intOr(root, "base_entities", baseEntities));
        maxEntities = Math.max(baseEntities, intOr(root, "max_entities", maxEntities));
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0, object.get(key).getAsInt());
    }

    private static double doubleOr(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0.0, object.get(key).getAsDouble());
    }
}
