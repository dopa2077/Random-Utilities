package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.minichest.MiniChestBlockEntity;
import com.dopa.randomutilities.itemcollector.ItemCollectorBlockEntity;
import com.dopa.randomutilities.filter.dev.UiTestBlockEntity;
import com.dopa.randomutilities.machine.generator.ResourceGeneratorBlockEntity;
import com.dopa.randomutilities.trashcan.TrashCanBlockEntity;
import com.dopa.randomutilities.redstoneclock.RedstoneClockBlockEntity;
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

    public static final Supplier<BlockEntityType<MiniChestBlockEntity>> MINI_CHEST =
            BLOCK_ENTITIES.register(
                    "mini_chest",
                    () -> new BlockEntityType<>(
                            MiniChestBlockEntity::new,
                            false,
                            ModBlocks.MINI_CHEST.get()
                    )
            );

    public static final Supplier<BlockEntityType<TrashCanBlockEntity>> TRASH_CAN =
            BLOCK_ENTITIES.register(
                    "trash_can",
                    () -> new BlockEntityType<>(
                            TrashCanBlockEntity::new,
                            false,
                            ModBlocks.TRASH_CAN.get()
                    )
            );

    public static final Supplier<BlockEntityType<RedstoneClockBlockEntity>> REDSTONE_CLOCK =
            BLOCK_ENTITIES.register(
                    "redstone_clock",
                    () -> new BlockEntityType<>(
                            RedstoneClockBlockEntity::new,
                            false,
                            ModBlocks.REDSTONE_CLOCK.get()
                    )
            );

    public static final Supplier<BlockEntityType<ItemCollectorBlockEntity>> ITEM_COLLECTOR =
            BLOCK_ENTITIES.register(
                    "item_collector",
                    () -> new BlockEntityType<>(
                            ItemCollectorBlockEntity::new,
                            false,
                            ModBlocks.BASIC_ITEM_COLLECTOR.get(),
                            ModBlocks.ADVANCED_ITEM_COLLECTOR.get()
                    )
            );

    private ModBlockEntities() {}
}
