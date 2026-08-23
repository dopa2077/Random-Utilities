package com.dopa.randomutilities.config;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Startup toggles for registering mod blocks and items.
 * File: {@code config/dopas_random_utilities/features.toml}
 */
public final class FeatureConfig {
    private static final Map<String, ModConfigSpec.ConfigValue<Boolean>> BLOCK_FLAGS = new HashMap<>();
    private static final Map<String, ModConfigSpec.ConfigValue<Boolean>> ITEM_FLAGS = new HashMap<>();

    private FeatureConfig() {}

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Set entries to false to disable registration (content will not exist in the game).");
        builder.push("blocks");
        for (String id : ModContentIds.allBlockIds()) {
            BLOCK_FLAGS.put(id, builder.define(id, true));
        }
        builder.pop();
        builder.comment("Standalone items (block items are controlled by their block entry).");
        builder.push("items");
        for (String id : ModContentIds.allItemIds()) {
            ITEM_FLAGS.put(id, builder.define(id, true));
        }
        builder.pop();
        ModConfigSpec spec = builder.build();
        container.registerConfig(
                ModConfig.Type.STARTUP,
                spec,
                "dopas_random_utilities/features.toml"
        );
    }

    public static boolean isBlockEnabled(String id) {
        return isEnabled(BLOCK_FLAGS, id);
    }

    public static boolean isItemEnabled(String id) {
        return isEnabled(ITEM_FLAGS, id);
    }

    private static boolean isEnabled(Map<String, ModConfigSpec.ConfigValue<Boolean>> flags, String id) {
        ModConfigSpec.ConfigValue<Boolean> flag = flags.get(id);
        return flag != null && flag.get();
    }
}
