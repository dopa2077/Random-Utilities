package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.blockbreaker.AdvancedBlockBreakerBlock;
import com.dopa.randomutilities.blockbreaker.SimpleBlockBreakerBlock;
import com.dopa.randomutilities.blockplacer.AdvancedBlockPlacerBlock;
import com.dopa.randomutilities.blockplacer.SimpleBlockPlacerBlock;
import com.dopa.randomutilities.simpleframe.SimpleFrameBlock;
import com.dopa.randomutilities.minichest.MiniChestBlock;
import com.dopa.randomutilities.itemcollector.ItemCollectorBlock;
import com.dopa.randomutilities.itemcollector.ItemCollectorType;
import com.dopa.randomutilities.generator.ResourceGeneratorBlock;
import com.dopa.randomutilities.solarfurnace.SolarFurnaceBlock;
import com.dopa.randomutilities.fishnet.FishnetBlock;
import com.dopa.randomutilities.generator.config.GeneratorType;
import com.dopa.randomutilities.trashcan.TrashCanBlock;
import com.dopa.randomutilities.redstoneclock.RedstoneClockBlock;
import com.dopa.randomutilities.tinytnt.TinyTntBlock;
import com.dopa.randomutilities.transfer.TransferChannel;
import com.dopa.randomutilities.transfer.TransferNodeBlock;
import com.dopa.randomutilities.transfer.TransferPipeBlock;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(dOPasRandomUtilities.MOD_ID);
    private static final Map<GeneratorType, DeferredBlock<ResourceGeneratorBlock>> BY_TYPE = new EnumMap<>(GeneratorType.class);

    public static final DeferredBlock<MiniChestBlock> MINI_CHEST = BLOCKS.registerBlock(
            "mini_chest",
            MiniChestBlock::new,
            props -> props.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)
    );

    public static final DeferredBlock<TrashCanBlock> TRASH_CAN = BLOCKS.registerBlock(
            "trash_can",
            TrashCanBlock::new,
            props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion()
    );

    public static final DeferredBlock<RedstoneClockBlock> REDSTONE_CLOCK = BLOCKS.registerBlock(
            "redstone_clock",
            RedstoneClockBlock::new,
            props -> props.mapColor(MapColor.STONE).strength(3.0F, 6.0F).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .isRedstoneConductor((state, level, pos) -> false)
    );

    public static final DeferredBlock<ItemCollectorBlock> BASIC_ITEM_COLLECTOR = BLOCKS.registerBlock(
            "basic_item_collector",
            props -> new ItemCollectorBlock(props, ItemCollectorType.BASIC),
            props -> props.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion()
    );

    public static final DeferredBlock<ItemCollectorBlock> ADVANCED_ITEM_COLLECTOR = BLOCKS.registerBlock(
            "advanced_item_collector",
            props -> new ItemCollectorBlock(props, ItemCollectorType.ADVANCED),
            props -> props.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion()
    );

    public static final DeferredBlock<SolarFurnaceBlock> SOLAR_FURNACE = BLOCKS.registerBlock(
            "solar_furnace",
            SolarFurnaceBlock::new,
            props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(SolarFurnaceBlock.LIT) ? 13 : 0)
    );

    public static final DeferredBlock<FishnetBlock> FISHNET = BLOCKS.registerBlock(
            "fishnet",
            FishnetBlock::new,
            props -> props.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()
    );

    public static final DeferredBlock<SimpleBlockBreakerBlock> SIMPLE_BLOCK_BREAKER = BLOCKS.registerBlock(
            "simple_block_breaker",
            SimpleBlockBreakerBlock::new,
            props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
    );

    public static final DeferredBlock<SimpleBlockPlacerBlock> SIMPLE_BLOCK_PLACER = BLOCKS.registerBlock(
            "simple_block_placer",
            SimpleBlockPlacerBlock::new,
            props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
    );

    public static final DeferredBlock<AdvancedBlockBreakerBlock> ADVANCED_BLOCK_BREAKER = BLOCKS.registerBlock(
            "advanced_block_breaker",
            AdvancedBlockBreakerBlock::new,
            props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
    );

    public static final DeferredBlock<AdvancedBlockPlacerBlock> ADVANCED_BLOCK_PLACER = BLOCKS.registerBlock(
            "advanced_block_placer",
            AdvancedBlockPlacerBlock::new,
            props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
    );

    public static final DeferredBlock<SimpleFrameBlock> SIMPLE_FRAME = BLOCKS.registerBlock(
            "simple_frame",
            SimpleFrameBlock::new,
            props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.GLASS).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false)
    );

    public static final DeferredBlock<TinyTntBlock> TINY_TNT = BLOCKS.registerBlock(
            "tiny_tnt",
            TinyTntBlock::new,
            props -> props.mapColor(MapColor.FIRE).instabreak().sound(SoundType.GRASS).ignitedByLava().noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false)
    );

    public static final DeferredBlock<TransferPipeBlock> TRANSFER_PIPE = BLOCKS.registerBlock(
            "transfer_pipe",
            props -> new TransferPipeBlock(props, TransferChannel.NONE),
            props -> props.mapColor(MapColor.STONE).strength(1.0F).sound(SoundType.STONE).noOcclusion().dynamicShape()
    );

    private static final Map<TransferChannel, DeferredBlock<TransferPipeBlock>> PIPES = new EnumMap<>(TransferChannel.class);

    static {
        PIPES.put(TransferChannel.NONE, TRANSFER_PIPE);
        for (TransferChannel channel : TransferChannel.dyed()) {
            TransferChannel color = channel;
            PIPES.put(color, BLOCKS.registerBlock(
                    color.blockId(),
                    props -> new TransferPipeBlock(props, color),
                    props -> props.mapColor(color.mapColor()).strength(1.0F).sound(SoundType.STONE).noOcclusion().dynamicShape()
            ));
        }
        for (GeneratorType type : GeneratorType.values()) {
            BY_TYPE.put(type, registerGenerator(type));
        }
    }

    public static DeferredBlock<TransferPipeBlock> pipe(TransferChannel channel) {
        return PIPES.getOrDefault(channel, TRANSFER_PIPE);
    }

    public static Iterable<DeferredBlock<TransferPipeBlock>> pipes() {
        return PIPES.values();
    }

    public static final DeferredBlock<TransferNodeBlock> TRANSFER_NODE = BLOCKS.registerBlock(
            "transfer_node",
            TransferNodeBlock::new,
            props -> props.mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE).noOcclusion().dynamicShape()
    );

    public static DeferredBlock<ResourceGeneratorBlock> forType(GeneratorType type) {
        return BY_TYPE.get(type);
    }

    public static final DeferredBlock<ResourceGeneratorBlock> BASIC_STONE_GENERATOR = forType(GeneratorType.BASIC_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> INTERMEDIATE_STONE_GENERATOR = forType(GeneratorType.INTERMEDIATE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> ADVANCED_STONE_GENERATOR = forType(GeneratorType.ADVANCED_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> ELITE_STONE_GENERATOR = forType(GeneratorType.ELITE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> ULTIMATE_STONE_GENERATOR = forType(GeneratorType.ULTIMATE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> CREATIVE_STONE_GENERATOR = forType(GeneratorType.CREATIVE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> RANDOM_ORE_GENERATOR = forType(GeneratorType.RANDOM_ORE);
    public static final DeferredBlock<ResourceGeneratorBlock> METAL_BLOCK_GENERATOR = forType(GeneratorType.METAL_BLOCK);
    public static final DeferredBlock<ResourceGeneratorBlock> CREATIVE_RANDOM_ORE_GENERATOR = forType(GeneratorType.CREATIVE_RANDOM_ORE);
    public static final DeferredBlock<ResourceGeneratorBlock> CREATIVE_METAL_BLOCK_GENERATOR = forType(GeneratorType.CREATIVE_METAL_BLOCK);

    private ModBlocks() {}

    private static DeferredBlock<ResourceGeneratorBlock> registerGenerator(GeneratorType type) {
        return BLOCKS.registerBlock(
                type.id(),
                props -> new ResourceGeneratorBlock(props, type),
                props -> props.mapColor(MapColor.STONE).strength(3.5F, 6.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE)
        );
    }

    public static Block[] allGenerators() {
        return BY_TYPE.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
    }
}
