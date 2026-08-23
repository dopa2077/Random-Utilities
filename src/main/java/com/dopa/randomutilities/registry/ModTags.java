package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> TRANSFER_PIPES_BLOCK = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "transfer_pipes")
    );
    public static final TagKey<Item> TRANSFER_PIPES = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "transfer_pipes")
    );

    private ModTags() {}
}
