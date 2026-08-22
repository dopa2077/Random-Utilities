package com.dopa.randomutilities.magnet;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.magnet.config.MagnetConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

/** Magnets accept Range, Overclock, and Stack upgrades. Caps come from {@link MagnetConfig}. */
public final class MagnetUpgradeInventory extends UpgradeInventory {
    public MagnetUpgradeInventory() {
        super(UpgradeConfig.UPGRADE_SLOT_COUNT, MagnetConfig::maxRangeUpgrades);
    }

    public void loadFrom(MagnetContents contents) {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = contents.upgrade(i);
            if (stack.isEmpty()) {
                set(i, ItemResource.EMPTY, 0);
            } else {
                set(i, ItemResource.of(stack), stack.getCount());
            }
        }
    }

    public List<ItemStack> snapshot() {
        List<ItemStack> stacks = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            stacks.add(stackInSlot(i));
        }
        return stacks;
    }

    @Override
    public int maxFor(Item item) {
        if (item == ModItems.RANGE_UPGRADE.get()) {
            return MagnetConfig.maxRangeUpgrades();
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return MagnetConfig.maxOverclock();
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
            return Math.max(MagnetConfig.maxRangeUpgrades(), Math.max(MagnetConfig.maxOverclock(), UpgradeConfig.MAX_STACK_UPGRADE));
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
        return Math.max(0, max - existing);
    }
}
