package com.dopa.randomutilities.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Ghost-slot blacklist/whitelist matching shared by item collectors and the trash can.
 * Empty filter list → allow everything. Whitelist → allow matches only. Blacklist → allow non-matches.
 */
public final class GhostItemFilter {
    private GhostItemFilter() {}

    public static boolean allows(ItemStack stack, NonNullList<ItemStack> filterSlots, boolean whitelistMode) {
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
