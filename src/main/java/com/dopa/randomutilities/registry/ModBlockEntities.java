package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.cardboardbox.CardboardBoxBlockEntity;
import com.dopa.randomutilities.config.ModContentIds;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, dOPasRandomUtilities.MOD_ID);

    public static Supplier<BlockEntityType<ResourceGeneratorBlockEntity>> RESOURCE_GENERATOR;
    public static Supplier<BlockEntityType<MiniChestBlockEntity>> MINI_CHEST;
    public static Supplier<BlockEntityType<TrashCanBlockEntity>> TRASH_CAN;
    public static Supplier<BlockEntityType<RedstoneClockBlockEntity>> REDSTONE_CLOCK;
    public static Supplier<BlockEntityType<ItemCollectorBlockEntity>> ITEM_COLLECTOR;
    public static Supplier<BlockEntityType<SolarFurnaceBlockEntity>> SOLAR_FURNACE;
    public static Supplier<BlockEntityType<FishnetBlockEntity>> FISHNET;
    public static Supplier<BlockEntityType<SimpleBlockBreakerBlockEntity>> SIMPLE_BLOCK_BREAKER;
    public static Supplier<BlockEntityType<SimpleBlockPlacerBlockEntity>> SIMPLE_BLOCK_PLACER;
    public static Supplier<BlockEntityType<AdvancedBlockBreakerBlockEntity>> ADVANCED_BLOCK_BREAKER;
    public static Supplier<BlockEntityType<AdvancedBlockPlacerBlockEntity>> ADVANCED_BLOCK_PLACER;
    public static Supplier<BlockEntityType<TransferNodeBlockEntity>> TRANSFER_NODE;
    public static Supplier<BlockEntityType<CardboardBoxBlockEntity>> CARDBOARD_BOX;

    private ModBlockEntities() {}

    public static void registerEnabled() {
        if (ModBlocks.hasAnyGenerator()) {
            RESOURCE_GENERATOR = BLOCK_ENTITIES.register(
                    "resource_generator",
                    () -> new BlockEntityType<>(
                            ResourceGeneratorBlockEntity::new,
                            false,
                            ModBlocks.allGenerators()
                    )
            );
        }
        if (ModBlocks.MINI_CHEST != null) {
            MINI_CHEST = BLOCK_ENTITIES.register(
                    "mini_chest",
                    () -> new BlockEntityType<>(
                            MiniChestBlockEntity::new,
                            false,
                            ModBlocks.MINI_CHEST.get()
                    )
            );
        }
        if (ModBlocks.TRASH_CAN != null) {
            TRASH_CAN = BLOCK_ENTITIES.register(
                    "trash_can",
                    () -> new BlockEntityType<>(
                            TrashCanBlockEntity::new,
                            false,
                            ModBlocks.TRASH_CAN.get()
                    )
            );
        }
        if (ModBlocks.REDSTONE_CLOCK != null) {
            REDSTONE_CLOCK = BLOCK_ENTITIES.register(
                    "redstone_clock",
                    () -> new BlockEntityType<>(
                            RedstoneClockBlockEntity::new,
                            false,
                            ModBlocks.REDSTONE_CLOCK.get()
                    )
            );
        }
        if (ModBlocks.BASIC_ITEM_COLLECTOR != null || ModBlocks.ADVANCED_ITEM_COLLECTOR != null) {
            ITEM_COLLECTOR = BLOCK_ENTITIES.register(
                    "item_collector",
                    () -> {
                        var blocks = new ArrayList<Block>();
                        if (ModBlocks.BASIC_ITEM_COLLECTOR != null) {
                            blocks.add(ModBlocks.BASIC_ITEM_COLLECTOR.get());
                        }
                        if (ModBlocks.ADVANCED_ITEM_COLLECTOR != null) {
                            blocks.add(ModBlocks.ADVANCED_ITEM_COLLECTOR.get());
                        }
                        return new BlockEntityType<>(
                                ItemCollectorBlockEntity::new,
                                false,
                                blocks.toArray(Block[]::new)
                        );
                    }
            );
        }
        if (ModBlocks.SOLAR_FURNACE != null) {
            SOLAR_FURNACE = BLOCK_ENTITIES.register(
                    "solar_furnace",
                    () -> new BlockEntityType<>(
                            SolarFurnaceBlockEntity::new,
                            false,
                            ModBlocks.SOLAR_FURNACE.get()
                    )
            );
        }
        if (ModBlocks.FISHNET != null) {
            FISHNET = BLOCK_ENTITIES.register(
                    "fishnet",
                    () -> new BlockEntityType<>(
                            FishnetBlockEntity::new,
                            false,
                            ModBlocks.FISHNET.get()
                    )
            );
        }
        if (ModBlocks.SIMPLE_BLOCK_BREAKER != null) {
            SIMPLE_BLOCK_BREAKER = BLOCK_ENTITIES.register(
                    "simple_block_breaker",
                    () -> new BlockEntityType<>(
                            SimpleBlockBreakerBlockEntity::new,
                            false,
                            ModBlocks.SIMPLE_BLOCK_BREAKER.get()
                    )
            );
        }
        if (ModBlocks.SIMPLE_BLOCK_PLACER != null) {
            SIMPLE_BLOCK_PLACER = BLOCK_ENTITIES.register(
                    "simple_block_placer",
                    () -> new BlockEntityType<>(
                            SimpleBlockPlacerBlockEntity::new,
                            false,
                            ModBlocks.SIMPLE_BLOCK_PLACER.get()
                    )
            );
        }
        if (ModBlocks.ADVANCED_BLOCK_BREAKER != null) {
            ADVANCED_BLOCK_BREAKER = BLOCK_ENTITIES.register(
                    "advanced_block_breaker",
                    () -> new BlockEntityType<>(
                            AdvancedBlockBreakerBlockEntity::new,
                            false,
                            ModBlocks.ADVANCED_BLOCK_BREAKER.get()
                    )
            );
        }
        if (ModBlocks.ADVANCED_BLOCK_PLACER != null) {
            ADVANCED_BLOCK_PLACER = BLOCK_ENTITIES.register(
                    "advanced_block_placer",
                    () -> new BlockEntityType<>(
                            AdvancedBlockPlacerBlockEntity::new,
                            false,
                            ModBlocks.ADVANCED_BLOCK_PLACER.get()
                    )
            );
        }
        if (ModBlocks.TRANSFER_NODE != null) {
            TRANSFER_NODE = BLOCK_ENTITIES.register(
                    "transfer_node",
                    () -> new BlockEntityType<>(
                            TransferNodeBlockEntity::new,
                            false,
                            ModBlocks.TRANSFER_NODE.get()
                    )
            );
        }
        if (ModBlocks.CARDBOARD_BOX != null) {
            CARDBOARD_BOX = BLOCK_ENTITIES.register(
                    ModContentIds.CARDBOARD_BOX,
                    () -> new BlockEntityType<>(
                            CardboardBoxBlockEntity::new,
                            false,
                            ModBlocks.CARDBOARD_BOX.get()
                    )
            );
        }
    }
}
