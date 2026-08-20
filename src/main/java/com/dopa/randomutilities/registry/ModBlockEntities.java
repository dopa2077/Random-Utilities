package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.blockbreaker.AdvancedBlockBreakerBlockEntity;
import com.dopa.randomutilities.blockbreaker.SimpleBlockBreakerBlockEntity;
import com.dopa.randomutilities.blockplacer.AdvancedBlockPlacerBlockEntity;
import com.dopa.randomutilities.blockplacer.SimpleBlockPlacerBlockEntity;
import com.dopa.randomutilities.minichest.MiniChestBlockEntity;
import com.dopa.randomutilities.itemcollector.ItemCollectorBlockEntity;
import com.dopa.randomutilities.generator.ResourceGeneratorBlockEntity;
import com.dopa.randomutilities.solarfurnace.SolarFurnaceBlockEntity;
import com.dopa.randomutilities.fishnet.FishnetBlockEntity;
import com.dopa.randomutilities.trashcan.TrashCanBlockEntity;
import com.dopa.randomutilities.redstoneclock.RedstoneClockBlockEntity;
import com.dopa.randomutilities.transfer.TransferNodeBlockEntity;
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

    public static final Supplier<BlockEntityType<SolarFurnaceBlockEntity>> SOLAR_FURNACE =
            BLOCK_ENTITIES.register(
                    "solar_furnace",
                    () -> new BlockEntityType<>(
                            SolarFurnaceBlockEntity::new,
                            false,
                            ModBlocks.SOLAR_FURNACE.get()
                    )
            );

    public static final Supplier<BlockEntityType<FishnetBlockEntity>> FISHNET =
            BLOCK_ENTITIES.register(
                    "fishnet",
                    () -> new BlockEntityType<>(
                            FishnetBlockEntity::new,
                            false,
                            ModBlocks.FISHNET.get()
                    )
            );

    public static final Supplier<BlockEntityType<SimpleBlockBreakerBlockEntity>> SIMPLE_BLOCK_BREAKER =
            BLOCK_ENTITIES.register(
                    "simple_block_breaker",
                    () -> new BlockEntityType<>(
                            SimpleBlockBreakerBlockEntity::new,
                            false,
                            ModBlocks.SIMPLE_BLOCK_BREAKER.get()
                    )
            );

    public static final Supplier<BlockEntityType<SimpleBlockPlacerBlockEntity>> SIMPLE_BLOCK_PLACER =
            BLOCK_ENTITIES.register(
                    "simple_block_placer",
                    () -> new BlockEntityType<>(
                            SimpleBlockPlacerBlockEntity::new,
                            false,
                            ModBlocks.SIMPLE_BLOCK_PLACER.get()
                    )
            );

    public static final Supplier<BlockEntityType<AdvancedBlockBreakerBlockEntity>> ADVANCED_BLOCK_BREAKER =
            BLOCK_ENTITIES.register(
                    "advanced_block_breaker",
                    () -> new BlockEntityType<>(
                            AdvancedBlockBreakerBlockEntity::new,
                            false,
                            ModBlocks.ADVANCED_BLOCK_BREAKER.get()
                    )
            );

    public static final Supplier<BlockEntityType<AdvancedBlockPlacerBlockEntity>> ADVANCED_BLOCK_PLACER =
            BLOCK_ENTITIES.register(
                    "advanced_block_placer",
                    () -> new BlockEntityType<>(
                            AdvancedBlockPlacerBlockEntity::new,
                            false,
                            ModBlocks.ADVANCED_BLOCK_PLACER.get()
                    )
            );

    public static final Supplier<BlockEntityType<TransferNodeBlockEntity>> TRANSFER_NODE =
            BLOCK_ENTITIES.register(
                    "transfer_node",
                    () -> new BlockEntityType<>(
                            TransferNodeBlockEntity::new,
                            false,
                            ModBlocks.TRANSFER_NODE.get()
                    )
            );

    private ModBlockEntities() {}
}
