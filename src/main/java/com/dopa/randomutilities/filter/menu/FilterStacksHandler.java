package com.dopa.randomutilities.filter.menu;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.dopa.randomutilities.filter.FilterNesting;
import com.dopa.randomutilities.filter.FilterRegistry;
import com.dopa.randomutilities.filter.config.DevNullConfig;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class FilterStacksHandler extends ItemStacksResourceHandler {
    private final IntSupplier maxStackSize;
    private final boolean basic;
    private final Supplier<ItemStack> host;
    private Runnable onChanged = () -> {};

    public FilterStacksHandler(
            NonNullList<ItemStack> stacks,
            IntSupplier maxStackSize,
            boolean basic,
            Supplier<ItemStack> host
    ) {
        super(stacks);
        this.maxStackSize = maxStackSize;
        this.basic = basic;
        this.host = host;
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
        if (resource.isEmpty()) {
            return true;
        }
        ItemStack stack = resource.toStack();
        if (!FilterRegistry.isFilterItem(stack)) {
            return true;
        }
        return FilterNesting.canAcceptNested(host.get(), stack);
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }
}
