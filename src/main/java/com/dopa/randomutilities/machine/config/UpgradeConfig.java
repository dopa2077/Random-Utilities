package com.dopa.randomutilities.machine.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.google.gson.JsonElement;
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
import java.util.EnumMap;
import java.util.Map;

/**
 * Upgrade percents and per-machine caps from
 * {@code config/dopas_random_utilities/items/upgrade.json}.
 * Missing config is copied from the bundled default; an existing file is never overwritten.
 */
public final class UpgradeConfig {
    public static final int UPGRADE_SLOT_COUNT = 6;
    /** Fortune Mesh never guarantees treasure; 9×10% default = 90%. */
    public static final int FORTUNE_MESH_MAX_CHANCE_PERCENT = 90;

    private static final String CONFIG_RELATIVE = "dopas_random_utilities/items/upgrade.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/upgrade.json";

    private static int productivityBonusPercent = 13;
    private static int overclockSpeedPercent = 9;
    private static int fortuneMeshTreasurePercent = 10;
    private static int maxFortuneMeshFishnet = 9;
    private static int maxTreasureMeshFishnet = 1;
    private static int maxProductivityFishnet = 9;
    private static int maxOverclockFishnet = 15;
    private static int maxOverclockSolarFurnace = 3;
    private static final Map<GeneratorType, Integer> maxPerType = new EnumMap<>(GeneratorType.class);

    static {
        applyBuiltInDefaults();
        loadDefaultsFromJar();
    }

    private UpgradeConfig() {}

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
            dOPasRandomUtilities.LOGGER.error("Failed to load upgrade config from {}, using defaults", configFile, exception);
            loadDefaultsFromJar();
        }
    }

    public static void reload() {
        load();
    }

    public static int productivityBonusPercent() {
        return productivityBonusPercent;
    }

    public static int overclockSpeedPercent() {
        return overclockSpeedPercent;
    }

    public static int fortuneMeshTreasurePercent() {
        return fortuneMeshTreasurePercent;
    }

    /** Treasure chance from installed Fortune Mesh, capped at {@link #FORTUNE_MESH_MAX_CHANCE_PERCENT}. */
    public static int fortuneMeshChancePercent(int meshCount) {
        int percent = fortuneMeshTreasurePercent * Math.max(0, meshCount);
        if (percent <= 0) {
            return 0;
        }
        return Math.min(FORTUNE_MESH_MAX_CHANCE_PERCENT, percent);
    }

    public static int maxFortuneMeshFishnet() {
        int configured = Math.max(0, maxFortuneMeshFishnet);
        int per = Math.max(0, fortuneMeshTreasurePercent);
        if (per <= 0) {
            return configured;
        }
        return Math.min(configured, FORTUNE_MESH_MAX_CHANCE_PERCENT / per);
    }

    public static int maxTreasureMeshFishnet() {
        return Math.max(0, maxTreasureMeshFishnet);
    }

    public static int maxProductivityFishnet() {
        return Math.max(0, maxProductivityFishnet);
    }

    public static int maxOverclockFishnet() {
        return Math.max(0, maxOverclockFishnet);
    }

    public static int maxOverclockSolarFurnace() {
        return Math.max(0, maxOverclockSolarFurnace);
    }

    public static int maxPerType(GeneratorType type) {
        return Math.max(0, maxPerType.getOrDefault(type, 0));
    }

    public static boolean upgradesEnabled(GeneratorType type) {
        return maxPerType(type) > 0;
    }

    /**
     * Peek produce amount for the current productivity bank without mutating it.
     * Uses fractional carry: {@code bank + base * bonusPercent} percent-units.
     */
    public static int peekBoostedAmount(int baseAmount, int productivityCount, int bank) {
        if (baseAmount <= 0) {
            return baseAmount;
        }
        int bonus = productivityBonusPercent * Math.max(0, productivityCount);
        if (bonus <= 0) {
            return baseAmount;
        }
        int safeBank = Math.max(0, bank);
        return baseAmount + (safeBank + baseAmount * bonus) / 100;
    }

    /**
     * Advance the productivity remainder bank after a successful craft.
     * Returns the new bank (0–99 leftover percent-units typically, can be larger before mod).
     */
    public static int advanceProductivityBank(int baseAmount, int productivityCount, int bank) {
        if (baseAmount <= 0) {
            return Math.max(0, bank);
        }
        int bonus = productivityBonusPercent * Math.max(0, productivityCount);
        if (bonus <= 0) {
            return Math.max(0, bank);
        }
        return (Math.max(0, bank) + baseAmount * bonus) % 100;
    }

    /**
     * Speed multiplier from overclocks. Below 100% total this matches the old
     * "shorten duration by X%" curve (50% → 2×, 90% → 10×). At 100% and beyond,
     * extra percent keeps adding speed instead of going negative (100% → 100×,
     * 135% → 135×).
     */
    public static float overclockSpeed(int overclockCount) {
        int reduction = overclockSpeedPercent * Math.max(0, overclockCount);
        if (reduction <= 0) {
            return 1.0F;
        }
        if (reduction < 100) {
            return 100.0F / (100 - reduction);
        }
        return reduction;
    }

    public static int effectiveTicks(int recipeTicks, int overclockCount) {
        if (recipeTicks <= 0) {
            return 1;
        }
        int reduction = overclockSpeedPercent * Math.max(0, overclockCount);
        if (reduction <= 0) {
            return recipeTicks;
        }
        if (reduction < 100) {
            return Math.max(1, recipeTicks * (100 - reduction) / 100);
        }
        return Math.max(1, (int) Math.round(recipeTicks / (double) reduction));
    }

    private static void loadDefaultsFromJar() {
        try (InputStream input = UpgradeConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled upgrade config defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = UpgradeConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote upgrade config at {}", configFile);
        }
    }

    private static void applyBuiltInDefaults() {
        productivityBonusPercent = 13;
        overclockSpeedPercent = 9;
        fortuneMeshTreasurePercent = 10;
        maxFortuneMeshFishnet = 9;
        maxTreasureMeshFishnet = 1;
        maxProductivityFishnet = 9;
        maxOverclockFishnet = 15;
        maxOverclockSolarFurnace = 3;
        maxPerType.clear();
        maxPerType.put(GeneratorType.BASIC_STONE, 5);
        maxPerType.put(GeneratorType.INTERMEDIATE_STONE, 10);
        maxPerType.put(GeneratorType.ADVANCED_STONE, 15);
        maxPerType.put(GeneratorType.ELITE_STONE, 20);
        maxPerType.put(GeneratorType.ULTIMATE_STONE, 32);
        maxPerType.put(GeneratorType.RANDOM_ORE, 10);
        maxPerType.put(GeneratorType.METAL_BLOCK, 15);
        maxPerType.put(GeneratorType.CREATIVE_STONE, 0);
        maxPerType.put(GeneratorType.CREATIVE_RANDOM_ORE, 0);
        maxPerType.put(GeneratorType.CREATIVE_METAL_BLOCK, 0);
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root.has("productivity_bonus_percent_per_upgrade")) {
            productivityBonusPercent = Math.max(0, root.get("productivity_bonus_percent_per_upgrade").getAsInt());
        } else if (root.has("capacity_bonus_percent_per_upgrade")) {
            productivityBonusPercent = Math.max(0, root.get("capacity_bonus_percent_per_upgrade").getAsInt());
        }
        if (root.has("overclock_speed_percent_per_upgrade")) {
            overclockSpeedPercent = Math.max(0, root.get("overclock_speed_percent_per_upgrade").getAsInt());
        }
        applyGeneratorCaps(root);
        applySolarFurnace(root);
        applyFishnet(root);
    }

    private static void applyGeneratorCaps(JsonObject root) {
        JsonObject generators = root.getAsJsonObject("generators");
        if (generators != null) {
            for (GeneratorType type : GeneratorType.values()) {
                JsonElement element = generators.get(type.id());
                if (element == null) {
                    continue;
                }
                if (element.isJsonObject() && element.getAsJsonObject().has("max_upgrades")) {
                    maxPerType.put(type, Math.max(0, element.getAsJsonObject().get("max_upgrades").getAsInt()));
                } else if (element.isJsonPrimitive()) {
                    maxPerType.put(type, Math.max(0, element.getAsInt()));
                }
            }
            return;
        }
        JsonObject caps = root.getAsJsonObject("max_upgrades_per_type");
        if (caps == null) {
            return;
        }
        for (GeneratorType type : GeneratorType.values()) {
            JsonElement element = caps.get(type.id());
            if (element != null && element.isJsonPrimitive()) {
                maxPerType.put(type, Math.max(0, element.getAsInt()));
            }
        }
    }

    private static void applySolarFurnace(JsonObject root) {
        JsonObject solar = root.getAsJsonObject("solar_furnace");
        if (solar != null) {
            maxOverclockSolarFurnace = intOr(solar, "max_overclock", maxOverclockSolarFurnace);
        }
    }

    private static void applyFishnet(JsonObject root) {
        JsonObject fishnet = root.getAsJsonObject("fishnet");
        if (fishnet != null) {
            maxProductivityFishnet = intOr(fishnet, "max_productivity", maxProductivityFishnet);
            maxOverclockFishnet = intOr(fishnet, "max_overclock", maxOverclockFishnet);
            JsonObject fortune = fishnet.getAsJsonObject("fortune_mesh");
            if (fortune != null) {
                fortuneMeshTreasurePercent = intOr(fortune, "treasure_percent_per_upgrade", fortuneMeshTreasurePercent);
                maxFortuneMeshFishnet = intOr(fortune, "max", maxFortuneMeshFishnet);
            }
            JsonObject treasure = fishnet.getAsJsonObject("treasure_mesh");
            if (treasure != null) {
                maxTreasureMeshFishnet = intOr(treasure, "max", maxTreasureMeshFishnet);
            }
            return;
        }
        JsonObject fortuneMesh = root.getAsJsonObject("fortune_mesh_upgrade");
        if (fortuneMesh != null) {
            fortuneMeshTreasurePercent = intOr(fortuneMesh, "treasure_percent_per_upgrade", fortuneMeshTreasurePercent);
            maxFortuneMeshFishnet = intOr(fortuneMesh, "max_per_fishnet", maxFortuneMeshFishnet);
        } else if (root.has("fortune_mesh_treasure_percent_per_upgrade")) {
            fortuneMeshTreasurePercent = Math.max(0, root.get("fortune_mesh_treasure_percent_per_upgrade").getAsInt());
        }
        JsonObject treasureMesh = root.getAsJsonObject("treasure_mesh_upgrade");
        if (treasureMesh != null) {
            maxTreasureMeshFishnet = intOr(treasureMesh, "max_per_fishnet", maxTreasureMeshFishnet);
        }
        JsonObject caps = root.getAsJsonObject("max_upgrades_per_type");
        if (fortuneMesh == null && caps != null && caps.has("fishnet") && caps.get("fishnet").isJsonPrimitive()) {
            maxFortuneMeshFishnet = Math.max(0, caps.get("fishnet").getAsInt());
        }
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0, object.get(key).getAsInt());
    }
}
