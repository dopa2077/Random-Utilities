package com.dopa.randomutilities.itemcollector;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.registry.ModItems;

import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.function.IntSupplier;

/** Collectors accept range upgrades only. Cap comes from {@link ItemCollectorType}. */
public final class CollectorUpgradeInventory extends UpgradeInventory {
    private final IntSupplier maxRangeUpgrades;

    public CollectorUpgradeInventory(int size, IntSupplier maxRangeUpgrades) {
        super(size, maxRangeUpgrades);
        this.maxRangeUpgrades = maxRangeUpgrades;
    }

    private int maxCap() {
        return Math.max(0, maxRangeUpgrades.getAsInt());
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return true;
        }
        if (!resource.is(ModItems.RANGE_UPGRADE.get())) {
            return false;
        }
        int max = maxCap();
        if (max <= 0) {
            return false;
        }
        int existing = countOf(ModItems.RANGE_UPGRADE.get());
        ItemResource current = getResource(index);
        if (!current.isEmpty() && current.is(ModItems.RANGE_UPGRADE.get())) {
            existing -= getAmountAsInt(index);
        }
        return existing < max;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        ItemResource effective = resource.isEmpty() ? getResource(index) : resource;
        if (effective.isEmpty()) {
            return maxCap();
        }
        if (!effective.is(ModItems.RANGE_UPGRADE.get())) {
            return 0;
        }
        int max = maxCap();
        int existing = countOf(ModItems.RANGE_UPGRADE.get());
        ItemResource current = getResource(index);
        if (!current.isEmpty() && current.is(ModItems.RANGE_UPGRADE.get())) {
            existing -= getAmountAsInt(index);
        }
        return Math.max(0, max - existing);
    }

    /** Drops excess range upgrades when the type cap shrinks (world migration). */
    public void trimToCap() {
        int max = maxCap();
        int total = countOf(ModItems.RANGE_UPGRADE.get());
        if (total <= max) {
            return;
        }
        int excess = total - max;
        for (int i = size() - 1; i >= 0 && excess > 0; i--) {
            ItemResource resource = getResource(i);
            if (resource.isEmpty() || !resource.is(ModItems.RANGE_UPGRADE.get())) {
                continue;
            }
            int amount = getAmountAsInt(i);
            int remove = Math.min(amount, excess);
            int keep = amount - remove;
            if (keep <= 0) {
                set(i, ItemResource.EMPTY, 0);
            } else {
                set(i, resource, keep);
            }
            excess -= remove;
        }
    }
}
