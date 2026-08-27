package com.dopa.randomutilities.core.machine.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.google.gson.JsonObject;

/** Shared powered-machine baselines ({@code blocks/powered_machines.json}). */
public final class PoweredMachinesConfig {
    private static final String RELATIVE = "blocks/powered_machines.json";
    private static final String DEFAULT = "/default/dopas_random_utilities/blocks/powered_machines.json";

    private static int baseTicks = 40;
    private static double overclockCostExponent = 1.09;

    static {
        loadDefaultsFromJar();
    }

    private PoweredMachinesConfig() {}

    public static void load() {
        ConfigPack.loadJson(RELATIVE, DEFAULT, PoweredMachinesConfig::applyJson, PoweredMachinesConfig::loadDefaultsFromJar);
    }

    public static void reload() {
        load();
    }

    public static int baseTicks() {
        return Math.max(1, baseTicks);
    }

    public static double overclockCostExponent() {
        return Math.max(0.0, overclockCostExponent);
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT, PoweredMachinesConfig::applyJson, PoweredMachinesConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        baseTicks = 40;
        overclockCostExponent = 1.09;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root == null) {
            return;
        }
        baseTicks = Math.max(1, ConfigPack.intOr(root, "base_ticks", baseTicks));
        overclockCostExponent = ConfigPack.doubleOr(root, "overclock_cost_exponent", overclockCostExponent);
    }
}
