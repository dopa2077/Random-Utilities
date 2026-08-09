package com.dopa.randomutilities.fishnet;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Accepts productivity, overclock, fortune mesh, and treasure mesh with independent caps. */
public final class FishnetUpgradeInventory extends UpgradeInventory {
    public static final int MAX_PRODUCTIVITY = 9;
    public static final int MAX_OVERCLOCK = 15;

    public FishnetUpgradeInventory(int size) {
        super(size, FishnetUpgradeInventory::largestCap);
    }

    public static int maxFortuneMesh() {
        return UpgradeConfig.maxFortuneMeshFishnet();
    }

    public static int maxTreasureMesh() {
        return UpgradeConfig.maxTreasureMeshFishnet();
    }

    private static int largestCap() {
        return Math.max(
                MAX_PRODUCTIVITY,
                Math.max(MAX_OVERCLOCK, Math.max(maxFortuneMesh(), maxTreasureMesh()))
        );
    }

    private int maxFor(Item item) {
        if (item == ModItems.PRODUCTIVITY_UPGRADE.get()) {
            return MAX_PRODUCTIVITY;
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return MAX_OVERCLOCK;
        }
        if (item == ModItems.FORTUNE_MESH_UPGRADE.get()) {
            return maxFortuneMesh();
        }
        if (item == ModItems.TREASURE_MESH_UPGRADE.get()) {
            return maxTreasureMesh();
        }
        return 0;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return true;
        }
        if (!isUpgradeItem(resource)) {
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
        if (!isUpgradeItem(effective)) {
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
}
