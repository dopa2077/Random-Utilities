package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.function.IntSupplier;

/** Energy, efficiency, and overclock upgrades for FE generators. */
public final class GeneratorUpgradeInventory extends UpgradeInventory {
    private final IntSupplier energyCap;
    private final IntSupplier efficiencyCap;
    private final IntSupplier overclockCap;

    public GeneratorUpgradeInventory(
            int size,
            IntSupplier energyCap,
            IntSupplier efficiencyCap,
            IntSupplier overclockCap
    ) {
        super(size, item -> 0);
        this.energyCap = energyCap;
        this.efficiencyCap = efficiencyCap;
        this.overclockCap = overclockCap;
    }

    public static GeneratorUpgradeInventory combustion(int size) {
        return new GeneratorUpgradeInventory(
                size,
                UpgradeConfig::maxEnergyCombustion,
                UpgradeConfig::maxEfficiencyCombustion,
                UpgradeConfig::maxOverclockCombustion
        );
    }

    public static GeneratorUpgradeInventory solarPanel(int size) {
        return new GeneratorUpgradeInventory(
                size,
                UpgradeConfig::maxEnergySolarPanel,
                UpgradeConfig::maxEfficiencySolarPanel,
                UpgradeConfig::maxOverclockSolarPanel
        );
    }

    private int largestCap() {
        return Math.max(energyCap.getAsInt(), Math.max(efficiencyCap.getAsInt(), overclockCap.getAsInt()));
    }

    @Override
    public int maxFor(Item item) {
        if (item == ModItems.ENERGY_UPGRADE.get()) {
            return Math.max(0, energyCap.getAsInt());
        }
        if (item == ModItems.EFFICIENCY_UPGRADE.get()) {
            return Math.max(0, efficiencyCap.getAsInt());
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return Math.max(0, overclockCap.getAsInt());
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

    public void trimInstalledCaps() {
        trimToCap(ModItems.ENERGY_UPGRADE.get());
        trimToCap(ModItems.EFFICIENCY_UPGRADE.get());
        trimToCap(ModItems.OVERCLOCK_UPGRADE.get());
    }
}
