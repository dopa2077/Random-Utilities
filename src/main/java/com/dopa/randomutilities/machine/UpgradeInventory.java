package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

/** Six upgrade slots with per-type caps from {@link UpgradeConfig}. */
public class UpgradeInventory extends ItemStacksResourceHandler {
    private final ToIntFunction<Item> capFor;
    private Runnable onChanged = () -> {};

    public UpgradeInventory(int size, IntSupplier maxPerType) {
        this(size, item -> maxPerType.getAsInt());
    }

    public UpgradeInventory(int size, ToIntFunction<Item> capFor) {
        super(size);
        this.capFor = capFor;
    }

    public int maxFor(Item item) {
        return Math.max(0, capFor.applyAsInt(item));
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

    public int fortuneMeshCount() {
        return countOf(ModItems.FORTUNE_MESH_UPGRADE.get());
    }

    public int treasureMeshCount() {
        return countOf(ModItems.TREASURE_MESH_UPGRADE.get());
    }

    public int energyCount() {
        return countOf(ModItems.ENERGY_UPGRADE.get());
    }

    public int efficiencyCount() {
        return countOf(ModItems.EFFICIENCY_UPGRADE.get());
    }

    public int rangeCount() {
        return countOf(ModItems.RANGE_UPGRADE.get());
    }

    public int stackCount() {
        return countOf(ModItems.STACK_UPGRADE.get());
    }

    public int fluidCapacityCount() {
        return countOf(ModItems.FLUID_CAPACITY_UPGRADE.get());
    }

    public static boolean isUpgradeItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.PRODUCTIVITY_UPGRADE.get())
                        || stack.is(ModItems.OVERCLOCK_UPGRADE.get())
                        || stack.is(ModItems.FORTUNE_MESH_UPGRADE.get())
                        || stack.is(ModItems.TREASURE_MESH_UPGRADE.get())
                        || stack.is(ModItems.ENERGY_UPGRADE.get())
                        || stack.is(ModItems.EFFICIENCY_UPGRADE.get())
                        || stack.is(ModItems.RANGE_UPGRADE.get())
                        || stack.is(ModItems.STACK_UPGRADE.get()));
    }

    public static boolean isUpgradeItem(ItemResource resource) {
        return !resource.isEmpty()
                && (resource.is(ModItems.PRODUCTIVITY_UPGRADE.get())
                        || resource.is(ModItems.OVERCLOCK_UPGRADE.get())
                        || resource.is(ModItems.FORTUNE_MESH_UPGRADE.get())
                        || resource.is(ModItems.TREASURE_MESH_UPGRADE.get())
                        || resource.is(ModItems.ENERGY_UPGRADE.get())
                        || resource.is(ModItems.EFFICIENCY_UPGRADE.get())
                        || resource.is(ModItems.RANGE_UPGRADE.get())
                        || resource.is(ModItems.STACK_UPGRADE.get()));
    }

    public static boolean isEnergyMachineUpgrade(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.ENERGY_UPGRADE.get())
                        || stack.is(ModItems.EFFICIENCY_UPGRADE.get())
                        || stack.is(ModItems.RANGE_UPGRADE.get())
                        || stack.is(ModItems.OVERCLOCK_UPGRADE.get()));
    }

    public static boolean isEnergyMachineUpgrade(ItemResource resource) {
        return !resource.isEmpty()
                && (resource.is(ModItems.ENERGY_UPGRADE.get())
                        || resource.is(ModItems.EFFICIENCY_UPGRADE.get())
                        || resource.is(ModItems.RANGE_UPGRADE.get())
                        || resource.is(ModItems.OVERCLOCK_UPGRADE.get()));
    }

    public static boolean isCollectorUpgrade(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.RANGE_UPGRADE.get()) || stack.is(ModItems.STACK_UPGRADE.get()));
    }

    /** Shared machine upgrades (generators / solar furnace). Fortune Mesh is fishnet-only. */
    public static boolean isSharedMachineUpgrade(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.PRODUCTIVITY_UPGRADE.get()) || stack.is(ModItems.OVERCLOCK_UPGRADE.get()));
    }

    public static boolean isSharedMachineUpgrade(ItemResource resource) {
        return !resource.isEmpty()
                && (resource.is(ModItems.PRODUCTIVITY_UPGRADE.get()) || resource.is(ModItems.OVERCLOCK_UPGRADE.get()));
    }

    public ItemStack stackInSlot(int index) {
        ItemResource resource = getResource(index);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(getAmountAsInt(index));
    }

    /** Inserts as many upgrades from {@code stack} as the type cap allows. Returns amount taken. */
    public int insertFrom(ItemStack stack) {
        if (!isUpgradeItem(stack)) {
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

    public void clearContents() {
        for (int i = 0; i < size(); i++) {
            if (!getResource(i).isEmpty()) {
                set(i, ItemResource.EMPTY, 0);
            }
        }
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return true;
        }
        if (!isSharedMachineUpgrade(resource)) {
            return false;
        }
        Item item = resource.getItem();
        int existing = countOf(item);
        ItemResource current = getResource(index);
        if (!current.isEmpty() && current.is(item)) {
            existing -= getAmountAsInt(index);
        }
        return existing < maxFor(item);
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        ItemResource effective = resource.isEmpty() ? getResource(index) : resource;
        if (effective.isEmpty()) {
            return Math.max(maxFor(ModItems.PRODUCTIVITY_UPGRADE.get()), maxFor(ModItems.OVERCLOCK_UPGRADE.get()));
        }
        if (!isSharedMachineUpgrade(effective)) {
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

    public void loadSlots(ValueInput input) {
        loadSlots(input, "Upgrade");
    }

    public void loadSlots(ValueInput input, String keyPrefix) {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = input.read(keyPrefix + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                set(i, ItemResource.EMPTY, 0);
            } else {
                set(i, ItemResource.of(stack), stack.getCount());
            }
        }
    }

    public void saveSlots(ValueOutput output) {
        saveSlots(output, "Upgrade");
    }

    public void saveSlots(ValueOutput output, String keyPrefix) {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                output.store(keyPrefix + i, ItemStack.CODEC, stack);
            }
        }
    }

    public void dropAt(Level level, BlockPos pos) {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = stackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        clearContents();
    }

    /** Removes excess of {@code item} when the install cap is lower than a loaded save. */
    public void trimToCap(Item item) {
        int max = maxFor(item);
        int total = countOf(item);
        if (total <= max) {
            return;
        }
        int excess = total - max;
        for (int i = size() - 1; i >= 0 && excess > 0; i--) {
            ItemResource resource = getResource(i);
            if (resource.isEmpty() || !resource.is(item)) {
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

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }
}
