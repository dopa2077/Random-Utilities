package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.block.cardboardbox.CardboardBoxItem;
import com.dopa.randomutilities.config.FeatureConfig;
import com.dopa.randomutilities.config.ModContentIds;
import com.dopa.randomutilities.core.filter.config.DevNullConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.core.filter.FilterItem;
import com.dopa.randomutilities.core.filter.FilterProfile;
import com.dopa.randomutilities.core.filter.FilterRegistry;
import com.dopa.randomutilities.item.devnull.AdvancedDevNullItem;
import com.dopa.randomutilities.item.devnull.DevNullItem;
import com.dopa.randomutilities.item.lasso.LassoItem;
import com.dopa.randomutilities.item.lasso.LassoTier;
import com.dopa.randomutilities.item.magnet.MagnetContents;
import com.dopa.randomutilities.item.magnet.MagnetItem;
import com.dopa.randomutilities.item.upgrade.MachineUpgradeItem;
import com.dopa.randomutilities.item.wrench.WrenchItem;
import com.dopa.randomutilities.logistics.transfer.TransferChannel;
import com.dopa.randomutilities.core.filter.TransferFilterContents;
import com.dopa.randomutilities.logistics.transfer.TransferFilterItem;
import com.dopa.randomutilities.logistics.transfer.TransferNodeItem;
import com.dopa.randomutilities.logistics.transfer.TransferPipeItem;
import com.dopa.randomutilities.core.util.DescribedBlockItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(dOPasRandomUtilities.MOD_ID);

    private static final Map<TransferChannel, DeferredItem<BlockItem>> PIPES = new EnumMap<>(TransferChannel.class);
    private static final Map<GeneratorType, DeferredItem<BlockItem>> GENERATORS = new EnumMap<>(GeneratorType.class);

    public static DeferredItem<DevNullItem> DEV_NULL;
    public static DeferredItem<AdvancedDevNullItem> ADVANCED_DEV_NULL;
    public static DeferredItem<BlockItem> MINI_CHEST;
    public static DeferredItem<BlockItem> TRASH_CAN;
    public static DeferredItem<BlockItem> REDSTONE_CLOCK;
    public static DeferredItem<BlockItem> BASIC_ITEM_COLLECTOR;
    public static DeferredItem<BlockItem> ADVANCED_ITEM_COLLECTOR;
    public static DeferredItem<BlockItem> SOLAR_FURNACE;
    public static DeferredItem<BlockItem> FISHNET;
    public static DeferredItem<BlockItem> SIMPLE_BLOCK_BREAKER;
    public static DeferredItem<BlockItem> SIMPLE_BLOCK_PLACER;
    public static DeferredItem<BlockItem> ADVANCED_BLOCK_BREAKER;
    public static DeferredItem<BlockItem> ADVANCED_BLOCK_PLACER;
    public static DeferredItem<BlockItem> COMBUSTION_GENERATOR;
    public static DeferredItem<BlockItem> SOLAR_PANEL_CONTROLLER;
    public static DeferredItem<BlockItem> SOLAR_PANEL_TIER1;
    public static DeferredItem<BlockItem> SOLAR_PANEL_TIER2;
    public static DeferredItem<BlockItem> SOLAR_PANEL_TIER3;
    public static DeferredItem<BlockItem> SIMPLE_CORE_FRAME;
    public static DeferredItem<BlockItem> ADVANCED_CORE_FRAME;
    public static DeferredItem<LassoItem> LASSO;
    public static DeferredItem<LassoItem> GOLDEN_LASSO;
    public static DeferredItem<LassoItem> CURSED_LASSO;
    public static DeferredItem<BlockItem> TINY_TNT;
    public static DeferredItem<BlockItem> TRANSFER_PIPE;
    public static DeferredItem<BlockItem> TRANSFER_NODE;
    public static DeferredItem<CardboardBoxItem> CARDBOARD_BOX;
    public static DeferredItem<TransferFilterItem> FILTER;
    public static DeferredItem<MagnetItem> ITEM_MAGNET;
    public static DeferredItem<WrenchItem> WRENCH;
    public static DeferredItem<Item> WOOD_CHIP;
    public static DeferredItem<Item> UPGRADE_CASING;
    public static DeferredItem<MachineUpgradeItem> PRODUCTIVITY_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> OVERCLOCK_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> FORTUNE_MESH_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> TREASURE_MESH_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> ENERGY_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> FLUID_CAPACITY_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> EFFICIENCY_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> RANGE_UPGRADE;
    public static DeferredItem<MachineUpgradeItem> STACK_UPGRADE;

    private ModItems() {}

    public static void registerEnabled() {
        if (FeatureConfig.isItemEnabled(ModContentIds.DEV_NULL)) {
            DEV_NULL = registerFilter(ModContentIds.DEV_NULL, DevNullItem::new, DevNullConfig.basicProfile());
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.ADVANCED_DEV_NULL)) {
            ADVANCED_DEV_NULL = registerFilter(
                    ModContentIds.ADVANCED_DEV_NULL,
                    AdvancedDevNullItem::new,
                    DevNullConfig.advancedProfile()
            );
        }
        MINI_CHEST = describedBlockIfPresent(ModBlocks.MINI_CHEST, "block.dopasrandomutilities.mini_chest.tooltip");
        TRASH_CAN = simpleBlockItemIfPresent(ModBlocks.TRASH_CAN);
        REDSTONE_CLOCK = describedBlockIfPresent(ModBlocks.REDSTONE_CLOCK, "block.dopasrandomutilities.redstone_clock.tooltip");
        BASIC_ITEM_COLLECTOR = describedBlockIfPresent(
                ModBlocks.BASIC_ITEM_COLLECTOR,
                "block.dopasrandomutilities.item_collector.tooltip"
        );
        ADVANCED_ITEM_COLLECTOR = describedBlockIfPresent(
                ModBlocks.ADVANCED_ITEM_COLLECTOR,
                "block.dopasrandomutilities.item_collector.tooltip"
        );
        SOLAR_FURNACE = describedBlockIfPresent(ModBlocks.SOLAR_FURNACE, "block.dopasrandomutilities.solar_furnace.tooltip");
        FISHNET = simpleBlockItemIfPresent(ModBlocks.FISHNET);
        SIMPLE_BLOCK_BREAKER = describedBlockIfPresent(
                ModBlocks.SIMPLE_BLOCK_BREAKER,
                "block.dopasrandomutilities.simple_block_breaker.tooltip"
        );
        SIMPLE_BLOCK_PLACER = describedBlockIfPresent(
                ModBlocks.SIMPLE_BLOCK_PLACER,
                "block.dopasrandomutilities.simple_block_placer.tooltip"
        );
        ADVANCED_BLOCK_BREAKER = describedBlockIfPresent(
                ModBlocks.ADVANCED_BLOCK_BREAKER,
                "block.dopasrandomutilities.advanced_block_breaker.tooltip"
        );
        ADVANCED_BLOCK_PLACER = describedBlockIfPresent(
                ModBlocks.ADVANCED_BLOCK_PLACER,
                "block.dopasrandomutilities.advanced_block_placer.tooltip"
        );
        COMBUSTION_GENERATOR = describedBlockIfPresent(
                ModBlocks.COMBUSTION_GENERATOR,
                "block.dopasrandomutilities.combustion_generator.tooltip"
        );
        SOLAR_PANEL_CONTROLLER = describedBlockIfPresent(
                ModBlocks.SOLAR_PANEL_CONTROLLER,
                "block.dopasrandomutilities.solar_panel_controller.tooltip"
        );
        SOLAR_PANEL_TIER1 = describedBlockIfPresent(
                ModBlocks.SOLAR_PANEL_TIER1,
                "block.dopasrandomutilities.solar_panel_tier1.tooltip"
        );
        SOLAR_PANEL_TIER2 = describedBlockIfPresent(
                ModBlocks.SOLAR_PANEL_TIER2,
                "block.dopasrandomutilities.solar_panel_tier2.tooltip"
        );
        SOLAR_PANEL_TIER3 = describedBlockIfPresent(
                ModBlocks.SOLAR_PANEL_TIER3,
                "block.dopasrandomutilities.solar_panel_tier3.tooltip"
        );
        SIMPLE_CORE_FRAME = simpleBlockItemIfPresent(ModBlocks.SIMPLE_CORE_FRAME);
        ADVANCED_CORE_FRAME = simpleBlockItemIfPresent(ModBlocks.ADVANCED_CORE_FRAME);
        CARDBOARD_BOX = cardboardBoxIfPresent(ModBlocks.CARDBOARD_BOX);
        if (FeatureConfig.isItemEnabled(ModContentIds.LASSO)) {
            LASSO = ITEMS.registerItem(
                    ModContentIds.LASSO,
                    props -> new LassoItem(props, LassoTier.BASIC),
                    props -> props.stacksTo(1).durability(10).setNoCombineRepair()
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.GOLDEN_LASSO)) {
            GOLDEN_LASSO = ITEMS.registerItem(
                    ModContentIds.GOLDEN_LASSO,
                    props -> new LassoItem(props, LassoTier.GOLDEN),
                    props -> props.stacksTo(1).setNoCombineRepair()
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.CURSED_LASSO)) {
            CURSED_LASSO = ITEMS.registerItem(
                    ModContentIds.CURSED_LASSO,
                    props -> new LassoItem(props, LassoTier.CURSED),
                    props -> props.stacksTo(1).setNoCombineRepair()
            );
        }
        TINY_TNT = simpleBlockItemIfPresent(ModBlocks.TINY_TNT);
        if (ModBlocks.TRANSFER_PIPE != null) {
            TRANSFER_PIPE = ITEMS.registerItem(
                    ModBlocks.TRANSFER_PIPE.getId().getPath(),
                    props -> new TransferPipeItem(
                            ModBlocks.TRANSFER_PIPE.get(),
                            props,
                            "block.dopasrandomutilities.transfer_pipe.tooltip"
                    )
            );
            PIPES.put(TransferChannel.NONE, TRANSFER_PIPE);
        }
        for (TransferChannel channel : TransferChannel.dyed()) {
            DeferredBlock<?> block = ModBlocks.pipe(channel);
            if (block == null) {
                continue;
            }
            TransferChannel color = channel;
            DeferredItem<BlockItem> pipeItem = ITEMS.registerItem(
                    color.blockId(),
                    props -> new TransferPipeItem(
                            block.get(),
                            props.overrideDescription("block.dopasrandomutilities." + color.blockId()),
                            "block.dopasrandomutilities.transfer_pipe.tooltip"
                    )
            );
            PIPES.put(color, pipeItem);
        }
        if (ModBlocks.TRANSFER_NODE != null) {
            TRANSFER_NODE = ITEMS.registerItem(
                    ModBlocks.TRANSFER_NODE.getId().getPath(),
                    props -> new TransferNodeItem(ModBlocks.TRANSFER_NODE.get(), props)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.FILTER)) {
            FILTER = ITEMS.registerItem(
                    ModContentIds.FILTER,
                    TransferFilterItem::new,
                    props -> props.stacksTo(1).component(ModDataComponents.TRANSFER_FILTER.get(), TransferFilterContents.EMPTY)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.ITEM_MAGNET)) {
            ITEM_MAGNET = ITEMS.registerItem(
                    ModContentIds.ITEM_MAGNET,
                    MagnetItem::new,
                    props -> props.stacksTo(1).component(ModDataComponents.MAGNET_CONTENTS.get(), MagnetContents.defaults())
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.WRENCH)) {
            WRENCH = ITEMS.registerItem(
                    ModContentIds.WRENCH,
                    WrenchItem::new,
                    props -> props.stacksTo(1)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.WOOD_CHIP)) {
            WOOD_CHIP = ITEMS.registerItem(ModContentIds.WOOD_CHIP, Item::new);
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.UPGRADE_CASING)) {
            UPGRADE_CASING = ITEMS.registerItem(ModContentIds.UPGRADE_CASING, Item::new);
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.PRODUCTIVITY_UPGRADE)) {
            PRODUCTIVITY_UPGRADE = ITEMS.registerItem(
                    ModContentIds.PRODUCTIVITY_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.PRODUCTIVITY)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.OVERCLOCK_UPGRADE)) {
            OVERCLOCK_UPGRADE = ITEMS.registerItem(
                    ModContentIds.OVERCLOCK_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.OVERCLOCK)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.FORTUNE_MESH_UPGRADE)) {
            FORTUNE_MESH_UPGRADE = ITEMS.registerItem(
                    ModContentIds.FORTUNE_MESH_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.FORTUNE_MESH)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.TREASURE_MESH_UPGRADE)) {
            TREASURE_MESH_UPGRADE = ITEMS.registerItem(
                    ModContentIds.TREASURE_MESH_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.TREASURE_MESH)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.ENERGY_UPGRADE)) {
            ENERGY_UPGRADE = ITEMS.registerItem(
                    ModContentIds.ENERGY_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.ENERGY)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.FLUID_CAPACITY_UPGRADE)) {
            FLUID_CAPACITY_UPGRADE = ITEMS.registerItem(
                    ModContentIds.FLUID_CAPACITY_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.FLUID_CAPACITY)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.EFFICIENCY_UPGRADE)) {
            EFFICIENCY_UPGRADE = ITEMS.registerItem(
                    ModContentIds.EFFICIENCY_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.EFFICIENCY)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.RANGE_UPGRADE)) {
            RANGE_UPGRADE = ITEMS.registerItem(
                    ModContentIds.RANGE_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.RANGE)
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.STACK_UPGRADE)) {
            STACK_UPGRADE = ITEMS.registerItem(
                    ModContentIds.STACK_UPGRADE,
                    props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.STACK)
            );
        }
        for (GeneratorType type : GeneratorType.values()) {
            DeferredBlock<?> block = ModBlocks.forType(type);
            if (block == null) {
                continue;
            }
            boolean creative = type == GeneratorType.CREATIVE_STONE
                    || type == GeneratorType.CREATIVE_RANDOM_ORE
                    || type == GeneratorType.CREATIVE_METAL_BLOCK;
            String tooltipKey = switch (type.mode()) {
                case RANDOM_ORE -> "block.dopasrandomutilities.random_ore_generator.tooltip";
                case METAL_BLOCK -> "block.dopasrandomutilities.metal_block_generator.tooltip";
                case RECIPE -> "block.dopasrandomutilities.stone_generator.tooltip";
            };
            GENERATORS.put(
                    type,
                    creative
                            ? describedBlock(block, tooltipKey, props -> props.rarity(Rarity.EPIC))
                            : describedBlock(block, tooltipKey)
            );
        }
    }

    private static @Nullable DeferredItem<BlockItem> describedBlockIfPresent(DeferredBlock<?> block, String tooltipKey) {
        return block == null ? null : describedBlock(block, tooltipKey);
    }

    private static @Nullable DeferredItem<BlockItem> simpleBlockItemIfPresent(DeferredBlock<?> block) {
        return block == null ? null : ITEMS.registerSimpleBlockItem(block);
    }

    private static @Nullable DeferredItem<CardboardBoxItem> cardboardBoxIfPresent(DeferredBlock<?> block) {
        if (block == null) {
            return null;
        }
        return ITEMS.registerItem(
                block.getId().getPath(),
                props -> new CardboardBoxItem(block.get(), props)
        );
    }

    private static DeferredItem<BlockItem> describedBlock(DeferredBlock<?> block, String tooltipKey) {
        return describedBlock(block, tooltipKey, props -> props);
    }

    private static DeferredItem<BlockItem> describedBlock(
            DeferredBlock<?> block,
            String tooltipKey,
            UnaryOperator<Item.Properties> properties
    ) {
        return ITEMS.registerItem(
                block.getId().getPath(),
                props -> new DescribedBlockItem(block.get(), properties.apply(props), tooltipKey)
        );
    }

    private static <T extends FilterItem> DeferredItem<T> registerFilter(
            String id,
            Function<Item.Properties, T> factory,
            FilterProfile profile
    ) {
        return ITEMS.registerItem(
                id,
                props -> {
                    T item = factory.apply(props);
                    FilterRegistry.register(item, profile);
                    return item;
                },
                props -> props.stacksTo(1).component(ModDataComponents.FILTER_CONTENTS.get(), profile.defaultContents())
        );
    }

    public static @Nullable DeferredItem<BlockItem> pipe(TransferChannel channel) {
        return PIPES.get(channel);
    }

    public static Iterable<DeferredItem<BlockItem>> pipes() {
        return PIPES.values();
    }

    public static @Nullable DeferredItem<BlockItem> forType(GeneratorType type) {
        return GENERATORS.get(type);
    }
}
