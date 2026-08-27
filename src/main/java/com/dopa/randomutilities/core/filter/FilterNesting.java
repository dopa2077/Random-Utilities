package com.dopa.randomutilities.core.filter;

import com.dopa.randomutilities.item.devnull.DevNullItem;
import com.dopa.randomutilities.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/** Classic OpenBlocks-style nesting rules for basic {@code /dev/null} only. */
public final class FilterNesting {
    public static final int ADVANCEMENT_DEPTH = 5;
    public static final int MAX_DEPTH = 8;

    private FilterNesting() {}

    public static boolean isBasicDevNull(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DevNullItem;
    }

    /**
     * Nesting depth of a basic {@code /dev/null}: empty or non-nested contents = 1;
     * each nested basic {@code /dev/null} in slot 0 adds one level.
     */
    public static int nestingDepth(ItemStack stack) {
        if (!isBasicDevNull(stack)) {
            return 0;
        }
        FilterContents contents = stack.get(ModDataComponents.FILTER_CONTENTS.get());
        if (contents == null || contents.slotCount() == 0) {
            return 1;
        }
        ItemStack inner = contents.stackInSlot(0);
        if (!isBasicDevNull(inner)) {
            return 1;
        }
        return 1 + nestingDepth(inner);
    }

    /** Whether {@code candidate} may be stored inside {@code host} as a nested filter item. */
    public static boolean canAcceptNested(ItemStack host, ItemStack candidate) {
        if (!isBasicDevNull(host) || !isBasicDevNull(candidate)) {
            return false;
        }
        return 1 + nestingDepth(candidate) <= MAX_DEPTH;
    }
}
