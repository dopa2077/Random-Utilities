package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.function.IntSupplier;

/** Six upgrade slots with per-type caps from {@link UpgradeConfig}. */
public class UpgradeInventory extends ItemStacksResourceHandler {
    private final IntSupplier maxPerType;
    private Runnable onChanged = () -> {};

    public UpgradeInventory(NonNullList<ItemStack> stacks, IntSupplier maxPerType) {
        super(stacks);
        this.maxPerType = maxPerType;
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public int countOf(Item item) {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            ItemResource resource = getResource(i);
            if (!resource.isEmpty() && resource.is(item)) {
                total += getAmountAsInt(i);
            }
        }
        return total;
    }

    public int productivityCount() {
        return countOf(ModItems.PRODUCTIVITY_UPGRADE.get());
    }

    public int overclockCount() {
        return countOf(ModItems.OVERCLOCK_UPGRADE.get());
    }

    public static boolean isUpgradeItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.PRODUCTIVITY_UPGRADE.get()) || stack.is(ModItems.OVERCLOCK_UPGRADE.get()));
    }

    public static boolean isUpgradeItem(ItemResource resource) {
        return !resource.isEmpty()
                && (resource.is(ModItems.PRODUCTIVITY_UPGRADE.get()) || resource.is(ModItems.OVERCLOCK_UPGRADE.get()));
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
        int existing = countOf(item);
        ItemResource current = getResource(index);
        if (!current.isEmpty() && current.is(item)) {
            existing -= getAmountAsInt(index);
        }
        return existing < maxPerType.getAsInt();
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        int max = maxPerType.getAsInt();
        if (max <= 0) {
            return 0;
        }
        ItemResource effective = resource.isEmpty() ? getResource(index) : resource;
        if (effective.isEmpty()) {
            return max;
        }
        if (!isUpgradeItem(effective)) {
            return 0;
        }
        Item item = effective.getItem();
        int existing = countOf(item);
        ItemResource current = getResource(index);
        if (!current.isEmpty() && current.is(item)) {
            existing -= getAmountAsInt(index);
        }
        return Math.max(0, max - existing);
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }
}
