package com.dopa.randomutilities.itemcollector;

import com.dopa.randomutilities.util.GhostItemFilter;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Filter slot matching for item collectors (blacklist / whitelist). */
public final class ItemCollectorFilter {
    private ItemCollectorFilter() {}

    public static boolean shouldCollect(
            ItemStack stack,
            NonNullList<ItemStack> filterSlots,
            boolean whitelistMode
    ) {
        return GhostItemFilter.allows(stack, filterSlots, whitelistMode);
    }
}
