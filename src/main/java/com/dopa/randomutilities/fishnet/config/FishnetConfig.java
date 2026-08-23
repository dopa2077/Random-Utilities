package com.dopa.randomutilities.fishnet.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.google.gson.JsonObject;

/** Fishnet options ({@code blocks/fishnet.json}). */
public final class FishnetConfig {
    private static final String RELATIVE = "blocks/fishnet.json";
    private static final String DEFAULT = "/default/dopas_random_utilities/blocks/fishnet.json";

    private static boolean preventRareLoot = false;

    static {
        loadDefaultsFromJar();
    }

    private FishnetConfig() {}

    public static void load() {
        ConfigPack.loadJson(RELATIVE, DEFAULT, FishnetConfig::applyJson, FishnetConfig::loadDefaultsFromJar);
    }

    public static void reload() {
        load();
    }

    /** When true, never rolls fishing treasure (open water and Fortune Mesh ignored). */
    public static boolean preventRareLoot() {
        return preventRareLoot;
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT, FishnetConfig::applyJson, FishnetConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        preventRareLoot = false;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root != null && root.has("prevent_rare_loot")) {
            preventRareLoot = root.get("prevent_rare_loot").getAsBoolean();
        }
    }
}
