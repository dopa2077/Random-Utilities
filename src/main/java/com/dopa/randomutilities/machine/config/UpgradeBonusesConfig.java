package com.dopa.randomutilities.machine.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Global percent bonuses per installed upgrade.
 * File: {@code config/dopas_random_utilities/upgrade_bonuses.toml}
 * See README.txt for details.
 */
public final class UpgradeBonusesConfig {
    private static ModConfigSpec.IntValue productivity;
    private static ModConfigSpec.IntValue overclock;
    private static ModConfigSpec.IntValue treasure;
    private static ModConfigSpec.IntValue energy;
    private static ModConfigSpec.IntValue efficiency;
    private static ModConfigSpec.IntValue range;
    private static ModConfigSpec.IntValue fluidCapacity;

    private UpgradeBonusesConfig() {}

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "Percent (or flat for range) granted by EACH installed upgrade of that type.",
                "Details: see config/dopas_random_utilities/README.txt"
        );
        builder.push("bonuses_percent_per_upgrade");
        productivity = builder
                .comment("Extra output percent units per productivity upgrade (generators, fishnet, solar).")
                .defineInRange("productivity", 13, 0, 1000);
        overclock = builder
                .comment("Craft/pull speed percent per overclock upgrade.")
                .defineInRange("overclock", 9, 0, 1000);
        treasure = builder
                .comment("Fortune Mesh treasure chance percent per mesh (capped in code at 90% total).")
                .defineInRange("treasure", 10, 0, 100);
        energy = builder
                .comment("Reserved energy-upgrade percent (transfer nodes use a fixed pi multiplier).")
                .defineInRange("energy", 25, 0, 1000);
        efficiency = builder
                .comment("FE cost reduction percent per efficiency upgrade on powered machines.")
                .defineInRange("efficiency", 6, 0, 100);
        range = builder
                .comment("Extra range blocks per range upgrade (not a percent).")
                .defineInRange("range", 1, 0, 64);
        fluidCapacity = builder
                .comment("Reserved fluid-capacity percent (transfer nodes use a fixed pi multiplier).")
                .defineInRange("fluid_capacity", 10, 0, 1000);
        builder.pop();
        container.registerConfig(
                ModConfig.Type.COMMON,
                builder.build(),
                "dopas_random_utilities/upgrade_bonuses.toml"
        );
    }

    public static int productivity() {
        return value(productivity, 13);
    }

    public static int overclock() {
        return value(overclock, 9);
    }

    public static int treasure() {
        return value(treasure, 10);
    }

    public static int energy() {
        return value(energy, 25);
    }

    public static int efficiency() {
        return value(efficiency, 6);
    }

    public static int range() {
        return value(range, 1);
    }

    public static int fluidCapacity() {
        return value(fluidCapacity, 10);
    }

    private static int value(ModConfigSpec.IntValue field, int fallback) {
        return field != null ? Math.max(0, field.get()) : fallback;
    }
}
