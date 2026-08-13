package com.dopa.randomutilities.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    /** Common fishing-rod tag so any mod rod in {@code #c:tools/fishing_rod} is accepted. */
    public static final TagKey<Item> FISHING_RODS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("c", "tools/fishing_rod")
    );

    private ModItemTags() {}
}
