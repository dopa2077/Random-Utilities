package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.block.ResourceGeneratorBlock;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(dOPasRandomUtilities.MOD_ID);

    public static final DeferredBlock<ResourceGeneratorBlock> BASIC_STONE_GENERATOR =
            registerGenerator(GeneratorType.BASIC_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> INTERMEDIATE_STONE_GENERATOR =
            registerGenerator(GeneratorType.INTERMEDIATE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> ADVANCED_STONE_GENERATOR =
            registerGenerator(GeneratorType.ADVANCED_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> ELITE_STONE_GENERATOR =
            registerGenerator(GeneratorType.ELITE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> ULTIMATE_STONE_GENERATOR =
            registerGenerator(GeneratorType.ULTIMATE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> CREATIVE_STONE_GENERATOR =
            registerGenerator(GeneratorType.CREATIVE_STONE);
    public static final DeferredBlock<ResourceGeneratorBlock> RANDOM_ORE_GENERATOR =
            registerGenerator(GeneratorType.RANDOM_ORE);
    public static final DeferredBlock<ResourceGeneratorBlock> METAL_BLOCK_GENERATOR =
            registerGenerator(GeneratorType.METAL_BLOCK);
    public static final DeferredBlock<ResourceGeneratorBlock> CREATIVE_RANDOM_ORE_GENERATOR =
            registerGenerator(GeneratorType.CREATIVE_RANDOM_ORE);
    public static final DeferredBlock<ResourceGeneratorBlock> CREATIVE_METAL_BLOCK_GENERATOR =
            registerGenerator(GeneratorType.CREATIVE_METAL_BLOCK);

    private ModBlocks() {}

    private static DeferredBlock<ResourceGeneratorBlock> registerGenerator(GeneratorType type) {
        Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, ResourceGeneratorBlock> factory =
                props -> new ResourceGeneratorBlock(props, type);
        return BLOCKS.registerBlock(
                type.id(),
                factory,
                props -> props
                        .mapColor(MapColor.STONE)
                        .strength(3.5F, 6.0F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE)
        );
    }
}
