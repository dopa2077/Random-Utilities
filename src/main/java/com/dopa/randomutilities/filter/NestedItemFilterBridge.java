package com.dopa.randomutilities.filter;

import com.dopa.randomutilities.util.GhostItemFilter;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Bridges nested filter cards and /dev/null filters for {@link GhostItemFilter}. */
public final class NestedItemFilterBridge implements GhostItemFilter.NestedItemFilter {
    public static final NestedItemFilterBridge INSTANCE = new NestedItemFilterBridge();

    private NestedItemFilterBridge() {}

    @Override
    public boolean isFilter(ItemStack stack) {
        return FilterStorage.isNestedFilterHost(stack);
    }

    @Override
    public boolean allows(ItemStack candidate, ItemStack filter, int depth) {
        return FilterStorage.matchesNested(candidate, filter, depth);
    }

    @Override
    public boolean allows(ItemResource candidate, ItemStack filter, int depth) {
        return FilterStorage.matchesNested(candidate, filter, depth);
    }
}
