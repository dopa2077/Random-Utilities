package com.dopa.randomutilities.filter;

import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.registry.ModDataComponents;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class FilterItemHandler extends ItemAccessResourceHandler<ItemResource> {
    private final Item validItem;

    public FilterItemHandler(ItemAccess itemAccess) {
        super(itemAccess, Math.max(1, FilterStorage.get(itemAccess.getResource().toStack()).slotCount()));
        this.validItem = itemAccess.getResource().getItem();
    }

    private FilterContents contents(ItemResource accessResource) {
        FilterProfile p = FilterRegistry.profile(validItem);
        FilterContents fallback = p != null ? p.defaultContents() : FilterContents.basicDefault();
        return accessResource.getOrDefault(ModDataComponents.FILTER_CONTENTS.get(), fallback);
    }

    @Override
    protected ItemResource getResourceFrom(ItemResource accessResource, int index) {
        return accessResource.is(validItem) ? contents(accessResource).slot(index).resource() : ItemResource.EMPTY;
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return accessResource.is(validItem) ? contents(accessResource).slot(index).count() : 0;
    }

    @Override
    @Nullable
    protected ItemResource update(ItemResource accessResource, int index, ItemResource newResource, int newAmount) {
        if (!accessResource.is(validItem)) {
            return ItemResource.EMPTY;
        }
        if (!newResource.isEmpty()) {
            ItemStack host = accessResource.toStack();
            ItemStack incoming = newResource.toStack();
            if (FilterRegistry.isFilterItem(incoming) && !FilterNesting.canAcceptNested(host, incoming)) {
                return ItemResource.EMPTY;
            }
        }
        ItemStack stack = accessResource.toStack();
        FilterContents updated = contents(accessResource).withSlot(index, newResource, newAmount);
        FilterStorage.set(stack, updated);
        FilterContents clamped = stack.get(ModDataComponents.FILTER_CONTENTS.get());
        if (clamped == null) {
            return ItemResource.EMPTY;
        }
        return accessResource.with(ModDataComponents.FILTER_CONTENTS.get(), clamped);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (!itemAccess.getResource().is(validItem)) {
            return false;
        }
        if (resource.isEmpty()) {
            return true;
        }
        ItemStack stack = resource.toStack();
        if (!FilterRegistry.isFilterItem(stack)) {
            return true;
        }
        return FilterNesting.canAcceptNested(itemAccess.getResource().toStack(), stack);
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        if (!itemAccess.getResource().is(validItem)) {
            return 0;
        }
        FilterContents contents = contents(itemAccess.getResource());
        FilterProfile profile = FilterRegistry.profile(validItem);
        boolean basic = profile == null || profile.isBasic();
        ItemResource effective = resource.isEmpty()
                ? contents.slot(index).resource()
                : resource;
        return DevNullConfig.effectiveSlotCapacity(effective, contents.maxStackSize(), basic);
    }
}
