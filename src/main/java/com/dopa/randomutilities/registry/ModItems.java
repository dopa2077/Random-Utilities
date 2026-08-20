package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.generator.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.FilterItem;
import com.dopa.randomutilities.filter.FilterProfile;
import com.dopa.randomutilities.filter.FilterRegistry;
import com.dopa.randomutilities.filter.item.AdvancedDevNullItem;
import com.dopa.randomutilities.filter.item.DevNullItem;
import com.dopa.randomutilities.machine.item.MachineUpgradeItem;
import com.dopa.randomutilities.transfer.HeadKind;
import com.dopa.randomutilities.transfer.TransferChannel;
import com.dopa.randomutilities.transfer.TransferFilterContents;
import com.dopa.randomutilities.transfer.TransferFilterItem;
import com.dopa.randomutilities.transfer.TransferNodeItem;
import com.dopa.randomutilities.transfer.TransferPipeItem;
import com.dopa.randomutilities.util.DescribedBlockItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(dOPasRandomUtilities.MOD_ID);

    public static final DeferredItem<DevNullItem> DEV_NULL =
            registerFilter("dev_null", DevNullItem::new, DevNullConfig.basicProfile());

    public static final DeferredItem<AdvancedDevNullItem> ADVANCED_DEV_NULL =
            registerFilter("advanced_dev_null", AdvancedDevNullItem::new, DevNullConfig.advancedProfile());

    public static final DeferredItem<BlockItem> MINI_CHEST =
            describedBlock(ModBlocks.MINI_CHEST, "block.dopasrandomutilities.mini_chest.tooltip");

    public static final DeferredItem<BlockItem> TRASH_CAN = ITEMS.registerSimpleBlockItem(ModBlocks.TRASH_CAN);

    public static final DeferredItem<BlockItem> REDSTONE_CLOCK =
            describedBlock(ModBlocks.REDSTONE_CLOCK, "block.dopasrandomutilities.redstone_clock.tooltip");

    public static final DeferredItem<BlockItem> BASIC_ITEM_COLLECTOR =
            describedBlock(ModBlocks.BASIC_ITEM_COLLECTOR, "block.dopasrandomutilities.item_collector.tooltip");

    public static final DeferredItem<BlockItem> ADVANCED_ITEM_COLLECTOR =
            describedBlock(ModBlocks.ADVANCED_ITEM_COLLECTOR, "block.dopasrandomutilities.item_collector.tooltip");

    public static final DeferredItem<BlockItem> SOLAR_FURNACE =
            describedBlock(ModBlocks.SOLAR_FURNACE, "block.dopasrandomutilities.solar_furnace.tooltip");

    public static final DeferredItem<BlockItem> FISHNET =
            ITEMS.registerSimpleBlockItem(ModBlocks.FISHNET);

    public static final DeferredItem<BlockItem> SIMPLE_BLOCK_BREAKER =
            describedBlock(ModBlocks.SIMPLE_BLOCK_BREAKER, "block.dopasrandomutilities.simple_block_breaker.tooltip");

    public static final DeferredItem<BlockItem> SIMPLE_BLOCK_PLACER =
            describedBlock(ModBlocks.SIMPLE_BLOCK_PLACER, "block.dopasrandomutilities.simple_block_placer.tooltip");

    public static final DeferredItem<BlockItem> ADVANCED_BLOCK_BREAKER =
            describedBlock(ModBlocks.ADVANCED_BLOCK_BREAKER, "block.dopasrandomutilities.advanced_block_breaker.tooltip");

    public static final DeferredItem<BlockItem> ADVANCED_BLOCK_PLACER =
            describedBlock(ModBlocks.ADVANCED_BLOCK_PLACER, "block.dopasrandomutilities.advanced_block_placer.tooltip");

    public static final DeferredItem<BlockItem> SIMPLE_FRAME =
            ITEMS.registerSimpleBlockItem(ModBlocks.SIMPLE_FRAME);

    public static final DeferredItem<BlockItem> TINY_TNT =
            ITEMS.registerSimpleBlockItem(ModBlocks.TINY_TNT);

    public static final DeferredItem<BlockItem> TRANSFER_PIPE = ITEMS.registerItem(
            ModBlocks.TRANSFER_PIPE.getId().getPath(),
            props -> new TransferPipeItem(
                    ModBlocks.TRANSFER_PIPE.get(),
                    props,
                    "block.dopasrandomutilities.transfer_pipe.tooltip"
            )
    );

    private static final Map<TransferChannel, DeferredItem<BlockItem>> PIPES = new EnumMap<>(TransferChannel.class);

    static {
        PIPES.put(TransferChannel.NONE, TRANSFER_PIPE);
        for (TransferChannel channel : TransferChannel.dyed()) {
            TransferChannel color = channel;
            PIPES.put(color, ITEMS.registerItem(
                    color.blockId(),
                    props -> new TransferPipeItem(
                            ModBlocks.pipe(color).get(),
                            props.overrideDescription("block.dopasrandomutilities." + color.blockId()),
                            "block.dopasrandomutilities.transfer_pipe.tooltip"
                    )
            ));
        }
    }

    public static DeferredItem<BlockItem> pipe(TransferChannel channel) {
        return PIPES.getOrDefault(channel, TRANSFER_PIPE);
    }

    public static Iterable<DeferredItem<BlockItem>> pipes() {
        return PIPES.values();
    }

    public static final DeferredItem<BlockItem> TRANSFER_NODE = ITEMS.registerItem(
            ModBlocks.TRANSFER_NODE.getId().getPath(),
            props -> new TransferNodeItem(
                    ModBlocks.TRANSFER_NODE.get(),
                    props,
                    "block.dopasrandomutilities.transfer_node.tooltip",
                    HeadKind.ITEM
            )
    );

    public static final DeferredItem<BlockItem> TRANSFER_NODE_FLUID = ITEMS.registerItem(
            "transfer_node_fluid",
            props -> new TransferNodeItem(
                    ModBlocks.TRANSFER_NODE.get(),
                    props.overrideDescription("item.dopasrandomutilities.transfer_node_fluid"),
                    "block.dopasrandomutilities.transfer_node_fluid.tooltip",
                    HeadKind.FLUID
            )
    );

    public static final DeferredItem<BlockItem> TRANSFER_NODE_ENERGY = ITEMS.registerItem(
            "transfer_node_energy",
            props -> new TransferNodeItem(
                    ModBlocks.TRANSFER_NODE.get(),
                    props.overrideDescription("item.dopasrandomutilities.transfer_node_energy"),
                    "block.dopasrandomutilities.transfer_node_energy.tooltip",
                    HeadKind.ENERGY
            )
    );

    private static final Map<HeadKind, DeferredItem<BlockItem>> NODES = new EnumMap<>(HeadKind.class);

    static {
        NODES.put(HeadKind.ITEM, TRANSFER_NODE);
        NODES.put(HeadKind.FLUID, TRANSFER_NODE_FLUID);
        NODES.put(HeadKind.ENERGY, TRANSFER_NODE_ENERGY);
    }

    public static DeferredItem<BlockItem> node(HeadKind kind) {
        return NODES.getOrDefault(kind, TRANSFER_NODE);
    }

    public static final DeferredItem<TransferFilterItem> FILTER = ITEMS.registerItem(
            "filter",
            TransferFilterItem::new,
            props -> props.stacksTo(1).component(ModDataComponents.TRANSFER_FILTER.get(), TransferFilterContents.EMPTY)
    );

    public static final DeferredItem<Item> WOOD_CHIP =
            ITEMS.registerItem("wood_chip", Item::new);

    public static final DeferredItem<Item> UPGRADE_CASING = ITEMS.registerItem("upgrade_casing", Item::new);
    public static final DeferredItem<MachineUpgradeItem> PRODUCTIVITY_UPGRADE = ITEMS.registerItem(
            "productivity_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.PRODUCTIVITY)
    );
    public static final DeferredItem<MachineUpgradeItem> OVERCLOCK_UPGRADE = ITEMS.registerItem(
            "overclock_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.OVERCLOCK)
    );
    public static final DeferredItem<MachineUpgradeItem> FORTUNE_MESH_UPGRADE = ITEMS.registerItem(
            "fortune_mesh_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.FORTUNE_MESH)
    );
    public static final DeferredItem<MachineUpgradeItem> TREASURE_MESH_UPGRADE = ITEMS.registerItem(
            "treasure_mesh_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.TREASURE_MESH)
    );
    public static final DeferredItem<MachineUpgradeItem> ENERGY_UPGRADE = ITEMS.registerItem(
            "energy_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.ENERGY)
    );
    public static final DeferredItem<MachineUpgradeItem> FLUID_CAPACITY_UPGRADE = ITEMS.registerItem(
            "fluid_capacity_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.FLUID_CAPACITY)
    );
    public static final DeferredItem<MachineUpgradeItem> EFFICIENCY_UPGRADE = ITEMS.registerItem(
            "efficiency_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.EFFICIENCY)
    );
    public static final DeferredItem<MachineUpgradeItem> RANGE_UPGRADE = ITEMS.registerItem(
            "range_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.RANGE)
    );
    public static final DeferredItem<MachineUpgradeItem> STACK_UPGRADE = ITEMS.registerItem(
            "stack_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.STACK)
    );

    private static final Map<GeneratorType, DeferredItem<BlockItem>> GENERATORS = new EnumMap<>(GeneratorType.class);

    static {
        for (GeneratorType type : GeneratorType.values()) {
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
                            ? describedBlock(ModBlocks.forType(type), tooltipKey, props -> props.rarity(Rarity.EPIC))
                            : describedBlock(ModBlocks.forType(type), tooltipKey)
            );
        }
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

    public static DeferredItem<BlockItem> forType(GeneratorType type) {
        return GENERATORS.get(type);
    }

    public static final DeferredItem<BlockItem> BASIC_STONE_GENERATOR = forType(GeneratorType.BASIC_STONE);
    public static final DeferredItem<BlockItem> INTERMEDIATE_STONE_GENERATOR = forType(GeneratorType.INTERMEDIATE_STONE);
    public static final DeferredItem<BlockItem> ADVANCED_STONE_GENERATOR = forType(GeneratorType.ADVANCED_STONE);
    public static final DeferredItem<BlockItem> ELITE_STONE_GENERATOR = forType(GeneratorType.ELITE_STONE);
    public static final DeferredItem<BlockItem> ULTIMATE_STONE_GENERATOR = forType(GeneratorType.ULTIMATE_STONE);
    public static final DeferredItem<BlockItem> CREATIVE_STONE_GENERATOR = forType(GeneratorType.CREATIVE_STONE);
    public static final DeferredItem<BlockItem> RANDOM_ORE_GENERATOR = forType(GeneratorType.RANDOM_ORE);
    public static final DeferredItem<BlockItem> METAL_BLOCK_GENERATOR = forType(GeneratorType.METAL_BLOCK);
    public static final DeferredItem<BlockItem> CREATIVE_RANDOM_ORE_GENERATOR = forType(GeneratorType.CREATIVE_RANDOM_ORE);
    public static final DeferredItem<BlockItem> CREATIVE_METAL_BLOCK_GENERATOR = forType(GeneratorType.CREATIVE_METAL_BLOCK);

    private ModItems() {}
}
