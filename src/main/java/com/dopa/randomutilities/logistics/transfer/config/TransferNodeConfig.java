package com.dopa.randomutilities.logistics.transfer.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.dopa.randomutilities.logistics.transfer.HeadKind;
import com.google.gson.JsonObject;

/** Transfer-node baselines ({@code blocks/transfer_node.json}). */
public final class TransferNodeConfig {
    private static final String RELATIVE = "blocks/transfer_node.json";
    private static final String DEFAULT = "/default/dopas_random_utilities/blocks/transfer_node.json";

    private static int itemBaseTicks = 40;
    private static int fluidBaseTicks = 60;
    private static int fluidBaseMb = 100;
    private static int energyBaseTicks = 20;
    private static int energyBaseFe = 200;

    static {
        loadDefaultsFromJar();
    }

    private TransferNodeConfig() {}

    public static void load() {
        ConfigPack.loadJson(RELATIVE, DEFAULT, TransferNodeConfig::applyJson, TransferNodeConfig::loadDefaultsFromJar);
    }

    public static void reload() {
        load();
    }

    public static int baseTicks(HeadKind kind) {
        return Math.max(1, switch (kind) {
            case ITEM -> itemBaseTicks;
            case FLUID -> fluidBaseTicks;
            case ENERGY -> energyBaseTicks;
        });
    }

    public static int baseMb() {
        return Math.max(0, fluidBaseMb);
    }

    public static int baseFe() {
        return Math.max(0, energyBaseFe);
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT, TransferNodeConfig::applyJson, TransferNodeConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        itemBaseTicks = 40;
        fluidBaseTicks = 60;
        fluidBaseMb = 100;
        energyBaseTicks = 20;
        energyBaseFe = 200;
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        if (root == null) {
            return;
        }
        JsonObject item = root.getAsJsonObject("item");
        if (item != null) {
            itemBaseTicks = Math.max(1, ConfigPack.intOr(item, "base_ticks", itemBaseTicks));
        }
        JsonObject fluid = root.getAsJsonObject("fluid");
        if (fluid != null) {
            fluidBaseTicks = Math.max(1, ConfigPack.intOr(fluid, "base_ticks", fluidBaseTicks));
            fluidBaseMb = ConfigPack.intOr(fluid, "base_mb", fluidBaseMb);
        }
        JsonObject energy = root.getAsJsonObject("energy");
        if (energy != null) {
            energyBaseTicks = Math.max(1, ConfigPack.intOr(energy, "base_ticks", energyBaseTicks));
            energyBaseFe = ConfigPack.intOr(energy, "base_fe", energyBaseFe);
        }
    }
}
