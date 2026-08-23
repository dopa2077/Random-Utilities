package com.dopa.randomutilities.config;

import com.dopa.randomutilities.generator.config.GeneratorType;
import com.dopa.randomutilities.transfer.TransferChannel;

import java.util.List;

/** Registry path ids for {@link FeatureConfig} enable flags. */
public final class ModContentIds {
    public static final String MINI_CHEST = "mini_chest";
    public static final String TRASH_CAN = "trash_can";
    public static final String REDSTONE_CLOCK = "redstone_clock";
    public static final String BASIC_ITEM_COLLECTOR = "basic_item_collector";
    public static final String ADVANCED_ITEM_COLLECTOR = "advanced_item_collector";
    public static final String SOLAR_FURNACE = "solar_furnace";
    public static final String FISHNET = "fishnet";
    public static final String SIMPLE_BLOCK_BREAKER = "simple_block_breaker";
    public static final String SIMPLE_BLOCK_PLACER = "simple_block_placer";
    public static final String ADVANCED_BLOCK_BREAKER = "advanced_block_breaker";
    public static final String ADVANCED_BLOCK_PLACER = "advanced_block_placer";
    public static final String COMBUSTION_GENERATOR = "combustion_generator";
    public static final String SOLAR_PANEL_CONTROLLER = "solar_panel_controller";
    public static final String SOLAR_PANEL_TIER1 = "solar_panel_tier1";
    public static final String SOLAR_PANEL_TIER2 = "solar_panel_tier2";
    public static final String SOLAR_PANEL_TIER3 = "solar_panel_tier3";
    public static final String SIMPLE_CORE_FRAME = "simple_core_frame";
    public static final String ADVANCED_CORE_FRAME = "advanced_core_frame";
    public static final String TINY_TNT = "tiny_tnt";
    public static final String TRANSFER_PIPE = "transfer_pipe";
    public static final String TRANSFER_NODE = "transfer_node";

    public static final String DEV_NULL = "dev_null";
    public static final String ADVANCED_DEV_NULL = "advanced_dev_null";
    public static final String LASSO = "lasso";
    public static final String GOLDEN_LASSO = "golden_lasso";
    public static final String CURSED_LASSO = "cursed_lasso";
    public static final String CARDBOARD_BOX = "cardboard_box";
    public static final String WOOD_CHIP = "wood_chip";
    public static final String UPGRADE_CASING = "upgrade_casing";
    public static final String PRODUCTIVITY_UPGRADE = "productivity_upgrade";
    public static final String OVERCLOCK_UPGRADE = "overclock_upgrade";
    public static final String FORTUNE_MESH_UPGRADE = "fortune_mesh_upgrade";
    public static final String TREASURE_MESH_UPGRADE = "treasure_mesh_upgrade";
    public static final String ENERGY_UPGRADE = "energy_upgrade";
    public static final String FLUID_CAPACITY_UPGRADE = "fluid_capacity_upgrade";
    public static final String EFFICIENCY_UPGRADE = "efficiency_upgrade";
    public static final String RANGE_UPGRADE = "range_upgrade";
    public static final String STACK_UPGRADE = "stack_upgrade";
    public static final String FILTER = "filter";
    public static final String ITEM_MAGNET = "item_magnet";
    public static final String TRANSFER_NODE_FLUID = "transfer_node_fluid";
    public static final String TRANSFER_NODE_ENERGY = "transfer_node_energy";

    public static final List<String> BLOCKS = List.of(
            MINI_CHEST,
            TRASH_CAN,
            REDSTONE_CLOCK,
            BASIC_ITEM_COLLECTOR,
            ADVANCED_ITEM_COLLECTOR,
            SOLAR_FURNACE,
            FISHNET,
            SIMPLE_BLOCK_BREAKER,
            SIMPLE_BLOCK_PLACER,
            ADVANCED_BLOCK_BREAKER,
            ADVANCED_BLOCK_PLACER,
            COMBUSTION_GENERATOR,
            SOLAR_PANEL_CONTROLLER,
            SOLAR_PANEL_TIER1,
            SOLAR_PANEL_TIER2,
            SOLAR_PANEL_TIER3,
            SIMPLE_CORE_FRAME,
            ADVANCED_CORE_FRAME,
            TINY_TNT,
            TRANSFER_PIPE,
            TRANSFER_NODE,
            CARDBOARD_BOX
    );

    public static final List<String> ITEMS = List.of(
            DEV_NULL,
            ADVANCED_DEV_NULL,
            LASSO,
            GOLDEN_LASSO,
            CURSED_LASSO,
            WOOD_CHIP,
            UPGRADE_CASING,
            PRODUCTIVITY_UPGRADE,
            OVERCLOCK_UPGRADE,
            FORTUNE_MESH_UPGRADE,
            TREASURE_MESH_UPGRADE,
            ENERGY_UPGRADE,
            FLUID_CAPACITY_UPGRADE,
            EFFICIENCY_UPGRADE,
            RANGE_UPGRADE,
            STACK_UPGRADE,
            FILTER,
            ITEM_MAGNET,
            TRANSFER_NODE_FLUID,
            TRANSFER_NODE_ENERGY
    );

    private ModContentIds() {}

    public static List<String> allBlockIds() {
        var ids = new java.util.ArrayList<>(BLOCKS);
        for (TransferChannel channel : TransferChannel.dyed()) {
            ids.add(channel.blockId());
        }
        for (GeneratorType type : GeneratorType.values()) {
            ids.add(type.id());
        }
        return List.copyOf(ids);
    }

    public static List<String> allItemIds() {
        return ITEMS;
    }
}
