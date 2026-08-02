package com.dopa.randomutilities.filteritem.menu;

import java.util.function.IntSupplier;

import com.dopa.randomutilities.filteritem.FilterRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class FilterStacksHandler extends ItemStacksResourceHandler {
    private final IntSupplier maxStackSize;
    private Runnable onChanged = () -> {};

    public FilterStacksHandler(NonNullList<ItemStack> stacks, IntSupplier maxStackSize) {
        super(stacks);
        this.maxStackSize = maxStackSize;
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return maxStackSize.getAsInt();
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || !FilterRegistry.isFilterItem(resource.toStack());
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }
}
