package com.dopa.randomutilities.filteritem;

import com.dopa.randomutilities.registry.ModDataComponents;

import net.minecraft.world.item.Item;
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
        if (!accessResource.is(validItem) || FilterRegistry.isFilterItem(newResource.toStack())) {
            return ItemResource.EMPTY;
        }
        FilterContents updated = contents(accessResource).withSlot(index, newResource, newAmount);
        return accessResource.with(ModDataComponents.FILTER_CONTENTS.get(), updated);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return itemAccess.getResource().is(validItem)
                && (resource.isEmpty() || !FilterRegistry.isFilterItem(resource.toStack()));
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return itemAccess.getResource().is(validItem) ? contents(itemAccess.getResource()).maxStackSize() : 0;
    }
}
