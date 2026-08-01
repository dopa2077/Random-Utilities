package com.dopa.randomutilities.filteritem;

import net.minecraft.network.chat.Component;

/** Capability flags for a filter-item variant. Define profiles on item classes in {@code item}, not here. */
public record FilterProfile(
        int minSlots,
        int maxSlots,
        int fixedMaxStack,
        boolean expandable,
        boolean colorable,
        boolean configurableMaxStack,
        boolean slotCycling,
        String emptyTooltipKey,
        String containerTitleKey,
        String slotsTooltipKey
) {
    public boolean isBasic() {
        return maxSlots <= 1 && !expandable;
    }

    public FilterContents defaultContents() {
        return isBasic() ? FilterContents.basicDefault() : FilterContents.advancedDefault(minSlots);
    }

    public Component containerTitle() {
        return Component.translatable(containerTitleKey);
    }
}
