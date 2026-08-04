package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.blockentity.UiTestBlockEntity;
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
                            ModBlocks.allGenerators()
                    )
            );

    public static final Supplier<BlockEntityType<UiTestBlockEntity>> UI_TEST =
            BLOCK_ENTITIES.register(
                    "ui_test",
                    () -> new BlockEntityType<>(
                            UiTestBlockEntity::new,
                            false,
                            ModBlocks.UI_TEST_BLOCK.get()
                    )
            );

    private ModBlockEntities() {}
}
