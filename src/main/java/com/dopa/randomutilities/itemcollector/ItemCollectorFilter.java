package com.dopa.randomutilities.itemcollector;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Filter slot matching for item collectors (blacklist / whitelist). */
public final class ItemCollectorFilter {
    private ItemCollectorFilter() {}

    public static boolean shouldCollect(
            ItemStack stack,
            NonNullList<ItemStack> filterSlots,
            boolean whitelistMode
    ) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean anyConfigured = false;
        boolean matches = false;
        for (ItemStack slot : filterSlots) {
            if (slot.isEmpty()) {
                continue;
            }
            anyConfigured = true;
            if (ItemResource.of(slot).matches(stack)) {
                matches = true;
                break;
            }
        }
        if (!anyConfigured) {
            return true;
        }
        return whitelistMode ? matches : !matches;
    }
}
