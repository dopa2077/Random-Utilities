package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(dOPasRandomUtilities.MOD_ID);

    public static final DeferredItem<BlockItem> BASIC_STONE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.BASIC_STONE_GENERATOR);
    public static final DeferredItem<BlockItem> INTERMEDIATE_STONE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.INTERMEDIATE_STONE_GENERATOR);
    public static final DeferredItem<BlockItem> ADVANCED_STONE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_STONE_GENERATOR);
    public static final DeferredItem<BlockItem> ELITE_STONE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.ELITE_STONE_GENERATOR);
    public static final DeferredItem<BlockItem> ULTIMATE_STONE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.ULTIMATE_STONE_GENERATOR);
    public static final DeferredItem<BlockItem> CREATIVE_STONE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_STONE_GENERATOR, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> RANDOM_ORE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.RANDOM_ORE_GENERATOR);
    public static final DeferredItem<BlockItem> METAL_BLOCK_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.METAL_BLOCK_GENERATOR);
    public static final DeferredItem<BlockItem> CREATIVE_RANDOM_ORE_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_RANDOM_ORE_GENERATOR, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> CREATIVE_METAL_BLOCK_GENERATOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_METAL_BLOCK_GENERATOR, props -> props.rarity(Rarity.EPIC));

    private ModItems() {}
}
