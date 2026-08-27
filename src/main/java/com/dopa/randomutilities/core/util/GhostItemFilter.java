package com.dopa.randomutilities.core.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

/**
 * Ghost-slot blacklist/whitelist matching shared by collectors, trash can, breakers, placers, and transfer nodes.
 * Empty filter list → allow everything. Whitelist → allow matches only. Blacklist → allow non-matches.
 * A nested Filter item in a ghost slot uses {@link NestedItemFilter} when registered.
 */
public final class GhostItemFilter {
    @Nullable
    private static NestedItemFilter nestedItemFilter;

    private GhostItemFilter() {}

    public static void setNestedItemFilter(@Nullable NestedItemFilter nested) {
        nestedItemFilter = nested;
    }

    public static boolean isNestedFilter(ItemStack stack) {
        return nestedItemFilter != null && nestedItemFilter.isFilter(stack);
    }

    public static boolean slotMatches(ItemStack ghost, ItemStack candidate) {
        return slotMatches(ghost, candidate, 0);
    }

    public static boolean slotMatches(ItemStack ghost, ItemStack candidate, int depth) {
        if (ghost.isEmpty() || candidate.isEmpty()) {
            return false;
        }
        NestedItemFilter nested = nestedItemFilter;
        if (nested != null && nested.isFilter(ghost)) {
            return nested.allows(candidate, ghost, depth);
        }
        return ItemResource.of(ghost).matches(candidate);
    }

    public static boolean slotMatches(ItemStack ghost, ItemResource candidate) {
        return slotMatches(ghost, candidate, 0);
    }

    public static boolean slotMatches(ItemStack ghost, ItemResource candidate, int depth) {
        if (ghost.isEmpty() || candidate.isEmpty()) {
            return false;
        }
        NestedItemFilter nested = nestedItemFilter;
        if (nested != null && nested.isFilter(ghost)) {
            return nested.allows(candidate, ghost, depth);
        }
        return ItemResource.of(ghost).equals(candidate);
    }

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
            if (slotMatches(slot, stack)) {
                matches = true;
                break;
            }
        }
        if (!anyConfigured) {
            return true;
        }
        return whitelistMode ? matches : !matches;
    }

    public static boolean allows(ItemResource resource, NonNullList<ItemStack> filterSlots, boolean whitelistMode) {
        if (resource.isEmpty()) {
            return false;
        }
        boolean anyConfigured = false;
        boolean matches = false;
        for (ItemStack slot : filterSlots) {
            if (slot.isEmpty()) {
                continue;
            }
            anyConfigured = true;
            if (slotMatches(slot, resource)) {
                matches = true;
                break;
            }
        }
        if (!anyConfigured) {
            return true;
        }
        return whitelistMode ? matches : !matches;
    }

    public interface NestedItemFilter {
        boolean isFilter(ItemStack stack);

        boolean allows(ItemStack candidate, ItemStack filter, int depth);

        /** Prefer this on hot paths to avoid {@code ItemResource.toStack(1)}. */
        default boolean allows(ItemResource candidate, ItemStack filter, int depth) {
            return allows(candidate.toStack(1), filter, depth);
        }
    }
}
