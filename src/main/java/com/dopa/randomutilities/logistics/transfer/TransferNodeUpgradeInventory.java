package com.dopa.randomutilities.logistics.transfer;

import com.dopa.randomutilities.core.machine.UpgradeInventory;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/** Overclock plus a kind-specific second upgrade for a transfer node head. */
public final class TransferNodeUpgradeInventory extends UpgradeInventory {
    private final Supplier<HeadKind> kind;

    public TransferNodeUpgradeInventory(Supplier<HeadKind> kind) {
        super(UpgradeConfig.UPGRADE_SLOT_COUNT, item -> 0);
        this.kind = kind;
    }

    public HeadKind kind() {
        return kind.get();
    }

    @Override
    public int maxFor(Item item) {
        HeadKind headKind = kind();
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return UpgradeConfig.maxOverclockTransferNode(headKind);
        }
        return switch (headKind) {
            case ITEM -> item == ModItems.STACK_UPGRADE.get() ? UpgradeConfig.MAX_STACK_UPGRADE : 0;
            case FLUID -> item == ModItems.FLUID_CAPACITY_UPGRADE.get() ? UpgradeConfig.maxFluidCapacity() : 0;
            case ENERGY -> item == ModItems.ENERGY_UPGRADE.get() ? UpgradeConfig.maxEnergyTransferNode() : 0;
        };
    }
}
