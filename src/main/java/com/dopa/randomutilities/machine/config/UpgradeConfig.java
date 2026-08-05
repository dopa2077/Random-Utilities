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

/** Generator upgrade limits from {@code config/dopas_random_utilities/items/upgrade.json}. */
public final class UpgradeConfig {
    public static final int UPGRADE_SLOT_COUNT = 6;

    private static final String CONFIG_RELATIVE = "dopas_random_utilities/items/upgrade.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/upgrade.json";

    private static int capacityBonusPercent = 10;
    private static int overclockSpeedPercent = 7;
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

    public static int capacityBonusPercent() {
        return capacityBonusPercent;
    }

    public static int overclockSpeedPercent() {
        return overclockSpeedPercent;
    }

    public static int maxPerType(GeneratorType type) {
        return Math.max(0, maxPerType.getOrDefault(type, 0));
    }

    public static boolean upgradesEnabled(GeneratorType type) {
        return maxPerType(type) > 0;
    }

    /**
     * Peek produce amount for the current capacity bank without mutating it.
     * Uses fractional carry: {@code bank + base * bonusPercent} percent-units.
     */
    public static int peekBoostedAmount(int baseAmount, int capacityCount, int bank) {
        if (baseAmount <= 0) {
            return baseAmount;
        }
        int bonus = capacityBonusPercent * Math.max(0, capacityCount);
        if (bonus <= 0) {
            return baseAmount;
        }
        int safeBank = Math.max(0, bank);
        return baseAmount + (safeBank + baseAmount * bonus) / 100;
    }

    /**
     * Advance the capacity remainder bank after a successful craft.
     * Returns the new bank (0–99 leftover percent-units typically, can be larger before mod).
     */
    public static int advanceCapacityBank(int baseAmount, int capacityCount, int bank) {
        if (baseAmount <= 0) {
            return Math.max(0, bank);
        }
        int bonus = capacityBonusPercent * Math.max(0, capacityCount);
        if (bonus <= 0) {
            return Math.max(0, bank);
        }
        return (Math.max(0, bank) + baseAmount * bonus) % 100;
    }

    /** Prefer {@link #peekBoostedAmount} with a remainder bank for amount-1 recipes. */
    @Deprecated
    public static int boostedAmount(int baseAmount, int capacityCount) {
        return peekBoostedAmount(baseAmount, capacityCount, 0);
    }

    public static int effectiveTicks(int recipeTicks, int overclockCount) {
        if (recipeTicks <= 0) {
            return 1;
        }
        if (overclockCount <= 0) {
            return recipeTicks;
        }
        return Math.max(1, recipeTicks * (100 - overclockSpeedPercent * overclockCount) / 100);
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
        capacityBonusPercent = 10;
        overclockSpeedPercent = 7;
        maxPerType.clear();
        maxPerType.put(GeneratorType.BASIC_STONE, 3);
        maxPerType.put(GeneratorType.INTERMEDIATE_STONE, 5);
        maxPerType.put(GeneratorType.ADVANCED_STONE, 7);
        maxPerType.put(GeneratorType.ELITE_STONE, 10);
        maxPerType.put(GeneratorType.ULTIMATE_STONE, 15);
        maxPerType.put(GeneratorType.RANDOM_ORE, 5);
        maxPerType.put(GeneratorType.METAL_BLOCK, 10);
        maxPerType.put(GeneratorType.CREATIVE_STONE, 0);
        maxPerType.put(GeneratorType.CREATIVE_RANDOM_ORE, 0);
        maxPerType.put(GeneratorType.CREATIVE_METAL_BLOCK, 0);
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root.has("capacity_bonus_percent_per_upgrade")) {
            capacityBonusPercent = Math.max(0, root.get("capacity_bonus_percent_per_upgrade").getAsInt());
        }
        if (root.has("overclock_speed_percent_per_upgrade")) {
            overclockSpeedPercent = Math.max(0, root.get("overclock_speed_percent_per_upgrade").getAsInt());
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
}
