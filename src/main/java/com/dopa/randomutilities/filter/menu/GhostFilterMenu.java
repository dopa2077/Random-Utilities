package com.dopa.randomutilities.filter.menu;

import net.minecraft.world.item.ItemStack;

/** Menus with ghost filter wells that accept serverbound filter slot updates. */
public interface GhostFilterMenu {
    void setFilterSlot(int index, ItemStack stack);

    int filterSlotCount();
}
