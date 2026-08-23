package com.dopa.randomutilities.solarfurnace;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Accepts overclock and productivity upgrades with independent caps. */
public final class SolarFurnaceUpgradeInventory extends UpgradeInventory {
    public SolarFurnaceUpgradeInventory(int size) {
        super(size, SolarFurnaceUpgradeInventory::largestCap);
    }

    public static int maxOverclock() {
        return UpgradeConfig.maxOverclockSolarFurnace();
    }

    public static int maxProductivity() {
        return UpgradeConfig.maxProductivitySolarFurnace();
    }

    private static int largestCap() {
        return Math.max(maxOverclock(), maxProductivity());
    }

    @Override
    public int maxFor(Item item) {
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return maxOverclock();
        }
        if (item == ModItems.PRODUCTIVITY_UPGRADE.get()) {
            return maxProductivity();
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
            return perSlotCapacity(largestCap());
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
}
