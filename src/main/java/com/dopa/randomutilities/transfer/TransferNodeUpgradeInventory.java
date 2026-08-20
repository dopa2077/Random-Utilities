package com.dopa.randomutilities.transfer;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Supplier;

/** Overclock plus a kind-specific second upgrade for a transfer node head. */
public final class TransferNodeUpgradeInventory extends UpgradeInventory {
    private final Supplier<HeadKind> kind;

    public TransferNodeUpgradeInventory(Supplier<HeadKind> kind) {
        super(UpgradeConfig.UPGRADE_SLOT_COUNT, TransferNodeUpgradeInventory::largestCap);
        this.kind = kind;
    }

    public HeadKind kind() {
        return kind.get();
    }

    public static int maxOverclock(HeadKind kind) {
        return UpgradeConfig.maxOverclockTransferNode(kind);
    }

    public static int maxStack() {
        return UpgradeConfig.MAX_STACK_UPGRADE;
    }

    public static boolean isNodeUpgrade(ItemStack stack) {
        return isNodeUpgrade(ItemResource.of(stack));
    }

    public static boolean isNodeUpgrade(ItemResource resource) {
        return !resource.isEmpty()
                && (resource.is(ModItems.OVERCLOCK_UPGRADE.get())
                        || resource.is(ModItems.STACK_UPGRADE.get())
                        || resource.is(ModItems.FLUID_CAPACITY_UPGRADE.get())
                        || resource.is(ModItems.ENERGY_UPGRADE.get()));
    }

    public static boolean isNodeUpgrade(HeadKind kind, ItemStack stack) {
        return isNodeUpgrade(kind, ItemResource.of(stack));
    }

    public static boolean isNodeUpgrade(HeadKind kind, ItemResource resource) {
        if (resource.isEmpty()) {
            return false;
        }
        if (resource.is(ModItems.OVERCLOCK_UPGRADE.get())) {
            return true;
        }
        return switch (kind) {
            case ITEM -> resource.is(ModItems.STACK_UPGRADE.get());
            case FLUID -> resource.is(ModItems.FLUID_CAPACITY_UPGRADE.get());
            case ENERGY -> resource.is(ModItems.ENERGY_UPGRADE.get());
        };
    }

    private static int largestCap() {
        return Math.max(
                UpgradeConfig.maxOverclockTransferNode(HeadKind.ITEM),
                Math.max(
                        UpgradeConfig.maxOverclockTransferNode(HeadKind.FLUID),
                        Math.max(
                                UpgradeConfig.maxOverclockTransferNode(HeadKind.ENERGY),
                                Math.max(
                                        maxStack(),
                                        Math.max(UpgradeConfig.maxFluidCapacity(), UpgradeConfig.maxEnergyTransferNode())
                                )
                        )
                )
        );
    }

    @Override
    public int maxFor(Item item) {
        HeadKind headKind = kind();
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return maxOverclock(headKind);
        }
        return switch (headKind) {
            case ITEM -> item == ModItems.STACK_UPGRADE.get() ? maxStack() : 0;
            case FLUID -> item == ModItems.FLUID_CAPACITY_UPGRADE.get() ? UpgradeConfig.maxFluidCapacity() : 0;
            case ENERGY -> item == ModItems.ENERGY_UPGRADE.get() ? UpgradeConfig.maxEnergyTransferNode() : 0;
        };
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return true;
        }
        if (!isNodeUpgrade(kind(), resource)) {
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
        if (!isNodeUpgrade(kind(), effective)) {
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

    @Override
    public int insertFrom(ItemStack stack) {
        if (!isNodeUpgrade(kind(), stack)) {
            return 0;
        }
        ItemResource resource = ItemResource.of(stack);
        try (Transaction tx = Transaction.open(null)) {
            int inserted = insert(resource, stack.getCount(), tx);
            if (inserted > 0) {
                tx.commit();
            }
            return Math.max(0, inserted);
        }
    }
}
