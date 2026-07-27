package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.block.BasicStoneGeneratorBlock;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(dOPasRandomUtilities.MOD_ID);

    public static final DeferredBlock<BasicStoneGeneratorBlock> BASIC_STONE_GENERATOR = BLOCKS.registerBlock(
            "basic_stone_generator",
            BasicStoneGeneratorBlock::new,
            props -> props
                    .mapColor(MapColor.STONE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
    );

    private ModBlocks() {}
}
