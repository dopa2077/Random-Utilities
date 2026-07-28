package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.blockentity.ResourceGeneratorBlockEntity;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, dOPasRandomUtilities.MOD_ID);

    public static final Supplier<BlockEntityType<ResourceGeneratorBlockEntity>> RESOURCE_GENERATOR =
            BLOCK_ENTITIES.register(
                    "resource_generator",
                    () -> new BlockEntityType<>(
                            ResourceGeneratorBlockEntity::new,
                            false,
                            ModBlocks.BASIC_STONE_GENERATOR.get(),
                            ModBlocks.INTERMEDIATE_STONE_GENERATOR.get(),
                            ModBlocks.ADVANCED_STONE_GENERATOR.get(),
                            ModBlocks.ELITE_STONE_GENERATOR.get(),
                            ModBlocks.ULTIMATE_STONE_GENERATOR.get(),
                            ModBlocks.CREATIVE_STONE_GENERATOR.get(),
                            ModBlocks.RANDOM_ORE_GENERATOR.get(),
                            ModBlocks.METAL_BLOCK_GENERATOR.get(),
                            ModBlocks.CREATIVE_RANDOM_ORE_GENERATOR.get(),
                            ModBlocks.CREATIVE_METAL_BLOCK_GENERATOR.get()
                    )
            );

    private ModBlockEntities() {}
}
