package com.dopa.randomutilities.filter.menu;

import com.dopa.randomutilities.filter.FilterContents;

/** Page / slot-count helpers for advanced filter menus. */
final class FilterPageLayout {
    private FilterPageLayout() {}

    static int slotsToAddForBulk(int slotCount) {
        int remainder = slotCount % FilterContents.SLOTS_PER_ROW;
        return remainder == 0 ? FilterContents.SLOTS_PER_ROW : FilterContents.SLOTS_PER_ROW - remainder;
    }

    static int slotsToRemoveForBulk(int slotCount) {
        int remainder = slotCount % FilterContents.SLOTS_PER_ROW;
        return remainder == 0 ? FilterContents.SLOTS_PER_ROW : remainder;
    }

    static int pageSlotCount(FilterContents contents, int page) {
        int start = page * FilterContents.SLOTS_PER_PAGE;
        int end = Math.min(contents.slotCount(), start + FilterContents.SLOTS_PER_PAGE);
        return Math.max(1, end - start);
    }

    static int slotX(int index) {
        return 8 + (index % FilterContents.SLOTS_PER_ROW) * 18;
    }

    static int slotY(int index) {
        return 18 + (index / FilterContents.SLOTS_PER_ROW) * 18;
    }
}
