package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.blockentity.BasicStoneGeneratorBlockEntity;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, dOPasRandomUtilities.MOD_ID);

    public static final Supplier<BlockEntityType<BasicStoneGeneratorBlockEntity>> BASIC_STONE_GENERATOR =
            BLOCK_ENTITIES.register(
                    "basic_stone_generator",
                    () -> new BlockEntityType<>(
                            BasicStoneGeneratorBlockEntity::new,
                            false,
                            ModBlocks.BASIC_STONE_GENERATOR.get()
                    )
            );

    private ModBlockEntities() {}
}
