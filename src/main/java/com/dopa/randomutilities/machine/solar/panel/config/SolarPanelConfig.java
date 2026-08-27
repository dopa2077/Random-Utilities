package com.dopa.randomutilities.machine.solar.panel.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.google.gson.JsonObject;

/** Solar panel controller baselines ({@code blocks/solar_panel.json}). */
public final class SolarPanelConfig {
    private static final String RELATIVE = "blocks/solar_panel.json";
    private static final String DEFAULT = "/default/dopas_random_utilities/blocks/solar_panel.json";

    private static int maxRange = 2;
    private static int tier1Fe = 5;
    private static int tier2Fe = 20;
    private static int tier3Fe = 100;
    private static int evalInterval = 10;

    static {
        loadDefaultsFromJar();
    }

    private SolarPanelConfig() {}

    public static void load() {
        ConfigPack.loadJson(RELATIVE, DEFAULT, SolarPanelConfig::applyJson, SolarPanelConfig::loadDefaultsFromJar);
    }

    public static void reload() {
        load();
    }

    public static int maxRange() {
        return Math.max(0, maxRange);
    }

    public static int tier1Fe() {
        return Math.max(0, tier1Fe);
    }

    public static int tier2Fe() {
        return Math.max(0, tier2Fe);
    }

    public static int tier3Fe() {
        return Math.max(0, tier3Fe);
    }

    public static int evalInterval() {
        return Math.max(1, evalInterval);
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT, SolarPanelConfig::applyJson, SolarPanelConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        maxRange = 2;
        tier1Fe = 5;
        tier2Fe = 20;
        tier3Fe = 100;
        evalInterval = 10;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root == null) {
            return;
        }
        maxRange = ConfigPack.intOr(root, "max_range", maxRange);
        tier1Fe = ConfigPack.intOr(root, "tier1_fe", tier1Fe);
        tier2Fe = ConfigPack.intOr(root, "tier2_fe", tier2Fe);
        tier3Fe = ConfigPack.intOr(root, "tier3_fe", tier3Fe);
        evalInterval = ConfigPack.intOr(root, "eval_interval", evalInterval);
    }
}
