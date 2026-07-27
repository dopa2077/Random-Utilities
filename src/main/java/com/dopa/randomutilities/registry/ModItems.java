package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(dOPasRandomUtilities.MOD_ID);

    public static final DeferredItem<BlockItem> BASIC_STONE_GENERATOR = ITEMS.registerSimpleBlockItem(
            ModBlocks.BASIC_STONE_GENERATOR
    );

    private ModItems() {}
}
