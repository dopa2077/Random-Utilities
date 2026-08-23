package com.dopa.randomutilities.itemcollector;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.function.IntSupplier;

/** Collectors accept Range and Stack upgrades. Range cap comes from {@link ItemCollectorType}. */
public final class CollectorUpgradeInventory extends UpgradeInventory {
    private final IntSupplier maxRangeUpgrades;

    public CollectorUpgradeInventory(int size, IntSupplier maxRangeUpgrades) {
        super(size, item -> Math.max(maxRangeUpgrades.getAsInt(), UpgradeConfig.MAX_STACK_UPGRADE));
        this.maxRangeUpgrades = maxRangeUpgrades;
    }

    private int maxRangeCap() {
        return Math.max(0, maxRangeUpgrades.getAsInt());
    }

    @Override
    public int maxFor(Item item) {
        if (item == ModItems.RANGE_UPGRADE.get()) {
            return maxRangeCap();
        }
        if (item == ModItems.STACK_UPGRADE.get()) {
            return UpgradeConfig.MAX_STACK_UPGRADE;
        }
        return 0;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return true;
        }
        Item item = resource.getItem();
        int max = maxFor(item);
        if (max <= 0) {
            return false;
        }
        int existing = countOf(item);
        ItemResource current = getResource(index);
        if (!current.isEmpty() && current.is(item)) {
            existing -= getAmountAsInt(index);
        }
        return existing < max;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        ItemResource effective = resource.isEmpty() ? getResource(index) : resource;
        if (effective.isEmpty()) {
            return perSlotCapacity(Math.max(maxRangeCap(), UpgradeConfig.MAX_STACK_UPGRADE));
        }
        Item item = effective.getItem();
        int max = maxFor(item);
        if (max <= 0) {
            return 0;
        }
        int existing = countOf(item);
        ItemResource current = getResource(index);
        if (!current.isEmpty() && current.is(item)) {
            existing -= getAmountAsInt(index);
        }
        return perSlotCapacity(max - existing);
    }

    /** Drops excess range upgrades when the type cap shrinks (world migration). */
    public void trimToCap() {
        int max = maxRangeCap();
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
