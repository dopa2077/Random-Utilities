package com.dopa.randomutilities.magnet.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.google.gson.JsonObject;

/**
 * Magnet behavior ({@code items/magnet.json}); upgrade caps ({@code upgrades/magnet.json}).
 */
public final class MagnetConfig {
    private static final String ITEMS_RELATIVE = "items/magnet.json";
    private static final String ITEMS_DEFAULT = "/default/dopas_random_utilities/items/magnet.json";
    private static final String UPGRADES_RELATIVE = "upgrades/magnet.json";
    private static final String UPGRADES_DEFAULT = "/default/dopas_random_utilities/upgrades/magnet.json";

    private static int baseRange = 4;
    private static int maxRange = 8;
    private static int maxRangeUpgrades = 16;
    private static int maxOverclock = 11;
    private static int baseTicks = 10;
    private static double pullSpeed = 0.22;
    private static int basePickupBatch = 1;
    private static int maxPickupBatch = 64;
    private static int baseEntities = 8;

    static {
        loadDefaultsFromJar();
    }

    private MagnetConfig() {}

    public static void load() {
        applyBuiltInDefaults();
        ConfigPack.loadJson(ITEMS_RELATIVE, ITEMS_DEFAULT, MagnetConfig::applyItemsJson, () -> {});
        ConfigPack.loadJson(UPGRADES_RELATIVE, UPGRADES_DEFAULT, MagnetConfig::applyUpgradesJson, MagnetConfig::loadDefaultsFromJar);
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

    public static int basePickupBatch() {
        return Math.max(1, basePickupBatch);
    }

    public static int maxPickupBatch() {
        return Math.max(basePickupBatch(), maxPickupBatch);
    }

    public static int baseEntities() {
        return Math.max(1, baseEntities);
    }

    private static void loadDefaultsFromJar() {
        applyBuiltInDefaults();
        ConfigPack.loadJarJson(ITEMS_DEFAULT, MagnetConfig::applyItemsJson, () -> {});
        ConfigPack.loadJarJson(UPGRADES_DEFAULT, MagnetConfig::applyUpgradesJson, () -> {});
    }

    private static void applyBuiltInDefaults() {
        baseRange = 4;
        maxRange = 8;
        maxRangeUpgrades = 16;
        maxOverclock = 11;
        baseTicks = 10;
        pullSpeed = 0.22;
        basePickupBatch = 1;
        maxPickupBatch = 64;
        baseEntities = 8;
    }

    private static void applyItemsJson(JsonObject root) {
        if (root == null) {
            return;
        }
        baseRange = ConfigPack.intOr(root, "base_range", baseRange);
        maxRange = ConfigPack.intOr(root, "max_range", maxRange);
        baseTicks = Math.max(1, ConfigPack.intOr(root, "base_ticks", baseTicks));
        pullSpeed = ConfigPack.doubleOr(root, "pull_speed", pullSpeed);
        basePickupBatch = Math.max(1, ConfigPack.intOr(root, "base_pickup_batch", basePickupBatch));
        maxPickupBatch = Math.max(basePickupBatch, ConfigPack.intOr(root, "max_pickup_batch", maxPickupBatch));
        baseEntities = Math.max(1, ConfigPack.intOr(root, "base_entities", baseEntities));
        if (root.has("max_entities") && !root.has("max_pickup_batch")) {
            maxPickupBatch = Math.max(basePickupBatch, ConfigPack.intOr(root, "max_entities", maxPickupBatch));
        }
    }

    private static void applyUpgradesJson(JsonObject root) {
        if (root == null) {
            return;
        }
        maxRangeUpgrades = ConfigPack.intOr(root, "max_range_upgrades", maxRangeUpgrades);
        maxOverclock = ConfigPack.intOr(root, "max_overclock", maxOverclock);
    }
}
