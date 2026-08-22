package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.cardboardbox.CardboardBoxBlock;
import com.dopa.randomutilities.blockbreaker.AdvancedBlockBreakerBlock;
import com.dopa.randomutilities.blockbreaker.SimpleBlockBreakerBlock;
import com.dopa.randomutilities.blockplacer.AdvancedBlockPlacerBlock;
import com.dopa.randomutilities.blockplacer.SimpleBlockPlacerBlock;
import com.dopa.randomutilities.config.FeatureConfig;
import com.dopa.randomutilities.config.ModContentIds;
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
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(dOPasRandomUtilities.MOD_ID);
    private static final Map<GeneratorType, DeferredBlock<ResourceGeneratorBlock>> BY_TYPE = new EnumMap<>(GeneratorType.class);
    private static final Map<TransferChannel, DeferredBlock<TransferPipeBlock>> PIPES = new EnumMap<>(TransferChannel.class);

    public static DeferredBlock<MiniChestBlock> MINI_CHEST;
    public static DeferredBlock<TrashCanBlock> TRASH_CAN;
    public static DeferredBlock<RedstoneClockBlock> REDSTONE_CLOCK;
    public static DeferredBlock<ItemCollectorBlock> BASIC_ITEM_COLLECTOR;
    public static DeferredBlock<ItemCollectorBlock> ADVANCED_ITEM_COLLECTOR;
    public static DeferredBlock<SolarFurnaceBlock> SOLAR_FURNACE;
    public static DeferredBlock<FishnetBlock> FISHNET;
    public static DeferredBlock<SimpleBlockBreakerBlock> SIMPLE_BLOCK_BREAKER;
    public static DeferredBlock<SimpleBlockPlacerBlock> SIMPLE_BLOCK_PLACER;
    public static DeferredBlock<AdvancedBlockBreakerBlock> ADVANCED_BLOCK_BREAKER;
    public static DeferredBlock<AdvancedBlockPlacerBlock> ADVANCED_BLOCK_PLACER;
    public static DeferredBlock<SimpleFrameBlock> SIMPLE_CORE_FRAME;
    public static DeferredBlock<SimpleFrameBlock> ADVANCED_CORE_FRAME;
    public static DeferredBlock<TinyTntBlock> TINY_TNT;
    public static DeferredBlock<TransferPipeBlock> TRANSFER_PIPE;
    public static DeferredBlock<TransferNodeBlock> TRANSFER_NODE;
    public static DeferredBlock<CardboardBoxBlock> CARDBOARD_BOX;

    private ModBlocks() {}

    public static void registerEnabled() {
        if (FeatureConfig.isBlockEnabled(ModContentIds.MINI_CHEST)) {
            MINI_CHEST = BLOCKS.registerBlock(
                    ModContentIds.MINI_CHEST,
                    MiniChestBlock::new,
                    props -> props.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.TRASH_CAN)) {
            TRASH_CAN = BLOCKS.registerBlock(
                    ModContentIds.TRASH_CAN,
                    TrashCanBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion()
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.REDSTONE_CLOCK)) {
            REDSTONE_CLOCK = BLOCKS.registerBlock(
                    ModContentIds.REDSTONE_CLOCK,
                    RedstoneClockBlock::new,
                    props -> props.mapColor(MapColor.STONE).strength(3.0F, 6.0F).sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
                            .isRedstoneConductor((state, level, pos) -> false)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.BASIC_ITEM_COLLECTOR)) {
            BASIC_ITEM_COLLECTOR = BLOCKS.registerBlock(
                    ModContentIds.BASIC_ITEM_COLLECTOR,
                    props -> new ItemCollectorBlock(props, ItemCollectorType.BASIC),
                    props -> props.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion()
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.ADVANCED_ITEM_COLLECTOR)) {
            ADVANCED_ITEM_COLLECTOR = BLOCKS.registerBlock(
                    ModContentIds.ADVANCED_ITEM_COLLECTOR,
                    props -> new ItemCollectorBlock(props, ItemCollectorType.ADVANCED),
                    props -> props.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion()
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.SOLAR_FURNACE)) {
            SOLAR_FURNACE = BLOCKS.registerBlock(
                    ModContentIds.SOLAR_FURNACE,
                    SolarFurnaceBlock::new,
                    props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                            .lightLevel(state -> state.getValue(SolarFurnaceBlock.LIT) ? 13 : 0)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.FISHNET)) {
            FISHNET = BLOCKS.registerBlock(
                    ModContentIds.FISHNET,
                    FishnetBlock::new,
                    props -> props.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.SIMPLE_BLOCK_BREAKER)) {
            SIMPLE_BLOCK_BREAKER = BLOCKS.registerBlock(
                    ModContentIds.SIMPLE_BLOCK_BREAKER,
                    SimpleBlockBreakerBlock::new,
                    props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.SIMPLE_BLOCK_PLACER)) {
            SIMPLE_BLOCK_PLACER = BLOCKS.registerBlock(
                    ModContentIds.SIMPLE_BLOCK_PLACER,
                    SimpleBlockPlacerBlock::new,
                    props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.ADVANCED_BLOCK_BREAKER)) {
            ADVANCED_BLOCK_BREAKER = BLOCKS.registerBlock(
                    ModContentIds.ADVANCED_BLOCK_BREAKER,
                    AdvancedBlockBreakerBlock::new,
                    props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.ADVANCED_BLOCK_PLACER)) {
            ADVANCED_BLOCK_PLACER = BLOCKS.registerBlock(
                    ModContentIds.ADVANCED_BLOCK_PLACER,
                    AdvancedBlockPlacerBlock::new,
                    props -> props.mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.SIMPLE_CORE_FRAME)) {
            SIMPLE_CORE_FRAME = BLOCKS.registerBlock(
                    ModContentIds.SIMPLE_CORE_FRAME,
                    SimpleFrameBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.GLASS).noOcclusion()
                            .isRedstoneConductor((state, level, pos) -> false)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.ADVANCED_CORE_FRAME)) {
            ADVANCED_CORE_FRAME = BLOCKS.registerBlock(
                    ModContentIds.ADVANCED_CORE_FRAME,
                    SimpleFrameBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(4.0F, 45.0F).sound(SoundType.GLASS).noOcclusion()
                            .isRedstoneConductor((state, level, pos) -> false)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.TINY_TNT)) {
            TINY_TNT = BLOCKS.registerBlock(
                    ModContentIds.TINY_TNT,
                    TinyTntBlock::new,
                    props -> props.mapColor(MapColor.FIRE).instabreak().sound(SoundType.GRASS).ignitedByLava().noOcclusion()
                            .isRedstoneConductor((state, level, pos) -> false)
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.TRANSFER_PIPE)) {
            TRANSFER_PIPE = BLOCKS.registerBlock(
                    ModContentIds.TRANSFER_PIPE,
                    props -> new TransferPipeBlock(props, TransferChannel.NONE),
                    props -> props.mapColor(MapColor.STONE).strength(1.0F).sound(SoundType.STONE).noOcclusion().dynamicShape()
            );
            PIPES.put(TransferChannel.NONE, TRANSFER_PIPE);
        }
        for (TransferChannel channel : TransferChannel.dyed()) {
            if (!FeatureConfig.isBlockEnabled(channel.blockId())) {
                continue;
            }
            TransferChannel color = channel;
            DeferredBlock<TransferPipeBlock> pipe = BLOCKS.registerBlock(
                    color.blockId(),
                    props -> new TransferPipeBlock(props, color),
                    props -> props.mapColor(color.mapColor()).strength(1.0F).sound(SoundType.STONE).noOcclusion().dynamicShape()
            );
            PIPES.put(color, pipe);
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.TRANSFER_NODE)) {
            TRANSFER_NODE = BLOCKS.registerBlock(
                    ModContentIds.TRANSFER_NODE,
                    TransferNodeBlock::new,
                    props -> props.mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE).noOcclusion().dynamicShape()
            );
        }
        if (FeatureConfig.isBlockEnabled(ModContentIds.CARDBOARD_BOX)) {
            CARDBOARD_BOX = BLOCKS.registerBlock(
                    ModContentIds.CARDBOARD_BOX,
                    CardboardBoxBlock::new,
                    props -> props.mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.WOOD).noLootTable()
            );
        }
        for (GeneratorType type : GeneratorType.values()) {
            if (FeatureConfig.isBlockEnabled(type.id())) {
                BY_TYPE.put(type, registerGenerator(type));
            }
        }
    }

    public static @Nullable DeferredBlock<TransferPipeBlock> pipe(TransferChannel channel) {
        return PIPES.get(channel);
    }

    public static Iterable<DeferredBlock<TransferPipeBlock>> pipes() {
        return PIPES.values();
    }

    public static @Nullable DeferredBlock<ResourceGeneratorBlock> forType(GeneratorType type) {
        return BY_TYPE.get(type);
    }

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

    public static boolean hasAnyGenerator() {
        return !BY_TYPE.isEmpty();
    }
}
