package com.dopa.randomutilities.filtersystem.menu;

import java.util.function.IntSupplier;

import com.dopa.randomutilities.config.DevNullConfig;
import com.dopa.randomutilities.filtersystem.FilterRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class FilterStacksHandler extends ItemStacksResourceHandler {
    private final IntSupplier maxStackSize;
    private final boolean basic;
    private Runnable onChanged = () -> {};

    public FilterStacksHandler(NonNullList<ItemStack> stacks, IntSupplier maxStackSize, boolean basic) {
        super(stacks);
        this.maxStackSize = maxStackSize;
        this.basic = basic;
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        int filterMax = maxStackSize.getAsInt();
        ItemResource effective = resource.isEmpty() ? getResource(index) : resource;
        return DevNullConfig.effectiveSlotCapacity(effective, filterMax, basic);
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
