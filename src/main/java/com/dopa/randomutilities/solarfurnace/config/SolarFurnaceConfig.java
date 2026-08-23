package com.dopa.randomutilities.solarfurnace.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.google.gson.JsonObject;

/** Solar furnace noon peak speed ({@code blocks/solar_furnace.json}). */
public final class SolarFurnaceConfig {
    private static final String RELATIVE = "blocks/solar_furnace.json";
    private static final String DEFAULT = "/default/dopas_random_utilities/blocks/solar_furnace.json";

    private static int peakSpeedPercent = 70;

    static {
        loadDefaultsFromJar();
    }

    private SolarFurnaceConfig() {}

    public static void load() {
        ConfigPack.loadJson(RELATIVE, DEFAULT, SolarFurnaceConfig::applyJson, SolarFurnaceConfig::loadDefaultsFromJar);
    }

    public static void reload() {
        load();
    }

    public static int peakSpeedPercent() {
        return Math.max(0, peakSpeedPercent);
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT, SolarFurnaceConfig::applyJson, SolarFurnaceConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        peakSpeedPercent = 70;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root == null) {
            return;
        }
        peakSpeedPercent = ConfigPack.intOr(root, "peak_speed_percent", peakSpeedPercent);
    }
}
