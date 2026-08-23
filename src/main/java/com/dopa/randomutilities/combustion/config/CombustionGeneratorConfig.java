package com.dopa.randomutilities.combustion.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.google.gson.JsonObject;

/** Combustion generator baselines ({@code blocks/combustion_generator.json}). */
public final class CombustionGeneratorConfig {
    private static final String RELATIVE = "blocks/combustion_generator.json";
    private static final String DEFAULT = "/default/dopas_random_utilities/blocks/combustion_generator.json";

    private static int baseFePerTick = 20;
    /**
     * Drain rate vs vanilla furnace burn time. With {@code base_fe_per_tick} 20 and factor 16,
     * coal (1600 burn) yields 2000 FE: {@code 1600 / 16 * 20}.
     */
    private static double burnSpeedFactor = 16.0;
    /** Base FE/t extract / push rate before energy upgrades. */
    private static int baseMaxExtract = 50;

    static {
        loadDefaultsFromJar();
    }

    private CombustionGeneratorConfig() {}

    public static void load() {
        ConfigPack.loadJson(RELATIVE, DEFAULT, CombustionGeneratorConfig::applyJson, CombustionGeneratorConfig::loadDefaultsFromJar);
    }

    public static void reload() {
        load();
    }

    public static int baseFePerTick() {
        return Math.max(0, baseFePerTick);
    }

    public static double burnSpeedFactor() {
        return Math.max(0.01, burnSpeedFactor);
    }

    public static int baseMaxExtract() {
        return Math.max(0, baseMaxExtract);
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT, CombustionGeneratorConfig::applyJson, CombustionGeneratorConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        baseFePerTick = 20;
        burnSpeedFactor = 16.0;
        baseMaxExtract = 50;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root == null) {
            return;
        }
        baseFePerTick = ConfigPack.intOr(root, "base_fe_per_tick", baseFePerTick);
        burnSpeedFactor = ConfigPack.doubleOr(root, "burn_speed_factor", burnSpeedFactor);
        baseMaxExtract = ConfigPack.intOr(root, "base_max_extract", baseMaxExtract);
    }
}
