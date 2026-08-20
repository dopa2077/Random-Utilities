package com.dopa.randomutilities.solarfurnace;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.registry.ModItems;

import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.function.IntSupplier;

/** Upgrade inventory that accepts overclock upgrades only. */
public final class OverclockUpgradeInventory extends UpgradeInventory {
    public OverclockUpgradeInventory(int size, IntSupplier maxPerType) {
        super(size, maxPerType);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return true;
        }
        if (!resource.is(ModItems.OVERCLOCK_UPGRADE.get())) {
            return false;
        }
        return super.isValid(index, resource);
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        ItemResource effective = resource.isEmpty() ? getResource(index) : resource;
        if (!effective.isEmpty() && !effective.is(ModItems.OVERCLOCK_UPGRADE.get())) {
            return 0;
        }
        return super.getCapacity(index, resource);
    }
}
