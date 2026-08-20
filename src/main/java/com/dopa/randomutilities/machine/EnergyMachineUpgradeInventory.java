package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Energy, efficiency, and range upgrades for powered volume machines. */
public final class EnergyMachineUpgradeInventory extends UpgradeInventory {
    public EnergyMachineUpgradeInventory(int size) {
        super(size, EnergyMachineUpgradeInventory::largestCap);
    }

    private static int largestCap() {
        return Math.max(
                UpgradeConfig.maxEnergy(),
                Math.max(UpgradeConfig.maxEfficiency(), UpgradeConfig.maxRange())
        );
    }

    @Override
    public int maxFor(Item item) {
        if (item == ModItems.ENERGY_UPGRADE.get()) {
            return UpgradeConfig.maxEnergy();
        }
        if (item == ModItems.EFFICIENCY_UPGRADE.get()) {
            return UpgradeConfig.maxEfficiency();
        }
        if (item == ModItems.RANGE_UPGRADE.get()) {
            return UpgradeConfig.maxRange();
        }
        return 0;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return true;
        }
        if (!isEnergyMachineUpgrade(resource)) {
            return false;
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
            return largestCap();
        }
        if (!isEnergyMachineUpgrade(effective)) {
            return 0;
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

    public void trimInstalledCaps() {
        trimToCap(ModItems.ENERGY_UPGRADE.get());
        trimToCap(ModItems.EFFICIENCY_UPGRADE.get());
        trimToCap(ModItems.RANGE_UPGRADE.get());
    }
}
