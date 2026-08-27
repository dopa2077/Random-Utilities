package com.dopa.randomutilities.core.machine;

import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

/** Upgrade wells whose allow-list is {@link #maxFor(Item)} (zero means rejected). */
public class UpgradeInventory extends ItemStacksResourceHandler {
    /** Hard per-well stack limit; type caps may still span multiple slots. */
    public static final int MAX_PER_SLOT = 64;

    private final ToIntFunction<Item> capFor;
    private Runnable onChanged = () -> {};

    public UpgradeInventory(int size, IntSupplier maxPerType) {
        this(size, item -> maxPerType.getAsInt());
    }

    public UpgradeInventory(int size, ToIntFunction<Item> capFor) {
        super(size);
        this.capFor = capFor;
    }

    public static UpgradeInventory withCaps(int size, ToIntFunction<Item> capFor) {
        return new UpgradeInventory(size, capFor);
    }

    /** Clamps type-cap room to one vanilla stack per upgrade well. */
    protected static int perSlotCapacity(int typeRoom) {
        return Math.min(MAX_PER_SLOT, Math.max(0, typeRoom));
    }

    public int maxFor(Item item) {
        return Math.max(0, capFor.applyAsInt(item));
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && maxFor(stack.getItem()) > 0;
    }

    public boolean accepts(ItemResource resource) {
        return !resource.isEmpty() && maxFor(resource.getItem()) > 0;
    }

    /**
     * Upgrade items this inventory accepts ({@code maxFor > 0}).
     * Overclock and productivity are listed first when present.
     */
    public List<Item> supportedUpgradeItems() {
        List<Item> supported = new ArrayList<>();
        Item overclock = ModItems.OVERCLOCK_UPGRADE.get();
        Item productivity = ModItems.PRODUCTIVITY_UPGRADE.get();
        if (maxFor(overclock) > 0) {
            supported.add(overclock);
        }
        if (maxFor(productivity) > 0) {
            supported.add(productivity);
        }
        for (Item item : upgradeCatalog()) {
            if (item == overclock || item == productivity) {
                continue;
            }
            if (maxFor(item) > 0) {
                supported.add(item);
            }
        }
        return supported;
    }

    private static Item[] upgradeCatalog() {
        return new Item[] {
                ModItems.OVERCLOCK_UPGRADE.get(),
                ModItems.PRODUCTIVITY_UPGRADE.get(),
                ModItems.FORTUNE_MESH_UPGRADE.get(),
                ModItems.TREASURE_MESH_UPGRADE.get(),
                ModItems.ENERGY_UPGRADE.get(),
                ModItems.EFFICIENCY_UPGRADE.get(),
                ModItems.RANGE_UPGRADE.get(),
                ModItems.STACK_UPGRADE.get(),
                ModItems.FLUID_CAPACITY_UPGRADE.get()
        };
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

    public ItemStack stackInSlot(int index) {
        ItemResource resource = getResource(index);
        if (resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack(getAmountAsInt(index));
    }

    public void loadStacks(List<ItemStack> stacks) {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                set(i, ItemResource.EMPTY, 0);
            } else {
                set(i, ItemResource.of(stack), Math.min(stack.getCount(), MAX_PER_SLOT));
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

    /** Inserts as many upgrades from {@code stack} as the type cap allows. Returns amount taken. */
    public int insertFrom(ItemStack stack) {
        if (!accepts(stack)) {
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

    private int largestCap() {
        int largest = 0;
        for (Item item : upgradeCatalog()) {
            largest = Math.max(largest, maxFor(item));
        }
        return largest;
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
                set(i, ItemResource.of(stack), Math.min(stack.getCount(), MAX_PER_SLOT));
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

    public void trimInstalledCaps() {
        for (Item item : upgradeCatalog()) {
            if (maxFor(item) >= 0) {
                trimToCap(item);
            }
        }
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }
}
