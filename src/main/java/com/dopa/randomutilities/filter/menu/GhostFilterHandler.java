package com.dopa.randomutilities.filter.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/** Capacity-1 ghost filter inventory used by machine/filter menus. */
public final class GhostFilterHandler extends ItemStacksResourceHandler {
    private Runnable onChanged = () -> {};

    public GhostFilterHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> {} : onChanged;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return 1;
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }
}
