package com.dopa.randomutilities.filteritem;

import com.dopa.randomutilities.config.DevNullConfig;
import com.dopa.randomutilities.filteritem.FilterContents.Slot;
import com.dopa.randomutilities.registry.ModDataComponents;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseRemainder;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class FilterStorage {
    private FilterStorage() {}

    public static FilterProfile profile(ItemStack stack) {
        return FilterRegistry.profile(stack);
    }

    public static FilterContents get(ItemStack stack) {
        FilterProfile profile = profile(stack);
        if (profile == null) {
            return FilterContents.basicDefault();
        }
        FilterContents stored = stack.getOrDefault(
                ModDataComponents.FILTER_CONTENTS.get(),
                profile.defaultContents()
        );
        FilterContents contents = profile.isBasic()
                ? clampBasic(stored, profile)
                : clampAdvanced(stored.ensureMinimum(profile.minSlots()), profile);
        if (!contents.equals(stored)) {
            stack.set(ModDataComponents.FILTER_CONTENTS.get(), contents);
        }
        return contents;
    }

    public static void set(ItemStack stack, FilterContents contents) {
        FilterProfile profile = profile(stack);
        if (profile == null) {
            return;
        }
        if (profile.isBasic()) {
            contents = clampBasic(new FilterContents(
                    contents.slots().isEmpty() ? List.of(Slot.EMPTY) : List.of(contents.slot(0)),
                    profile.fixedMaxStack() > 0 ? profile.fixedMaxStack() : contents.maxStackSize(),
                    0, 0, contents.color()
            ), profile);
        } else {
            contents = clampAdvanced(contents.ensureMinimum(profile.minSlots()), profile);
        }
        stack.set(ModDataComponents.FILTER_CONTENTS.get(), contents);
    }

    private static FilterContents clampBasic(FilterContents contents, FilterProfile profile) {
        int maxStack = Math.max(1, DevNullConfig.basicMaxStackSize());
        FilterContents clamped = contents.withMaxStackSize(Math.min(contents.maxStackSize(), maxStack));
        return new FilterContents(
                clamped.slots().isEmpty() ? List.of(Slot.EMPTY) : List.of(clamped.slot(0)),
                maxStack,
                0, 0, clamped.color()
        );
    }

    private static FilterContents clampAdvanced(FilterContents contents, FilterProfile profile) {
        int slots = DevNullConfig.clampAdvancedSlotCount(contents.slotCount());
        if (slots != contents.slotCount()) {
            contents = contents.withSlotCount(slots, DevNullConfig.advancedMinSlots(), DevNullConfig.advancedMaxSlots());
        }
        int maxStack = DevNullConfig.clampAdvancedMaxStack(contents.maxStackSize());
        if (maxStack != contents.maxStackSize()) {
            contents = contents.withMaxStackSize(maxStack);
        }
        int page = DevNullConfig.clampAdvancedPage(contents.page(), contents.slotCount());
        if (page != contents.page()) {
            contents = contents.withPage(page);
        }
        return contents;
    }

    public static ItemStack getSelectedStack(ItemStack host) {
        FilterContents contents = get(host);
        return contents.stackInSlot(contents.selectedSlot());
    }

    public static ItemStack getPreviewStack(ItemStack host) {
        if (!FilterRegistry.isFilterItem(host)) {
            return ItemStack.EMPTY;
        }
        FilterContents contents = get(host);
        FilterProfile profile = profile(host);
        if (profile != null && profile.isBasic()) {
            return contents.stackInSlot(0);
        }
        return contents.stackInSlot(contents.selectedSlot());
    }

    public static ItemStack getPreviewIconStack(ItemStack host) {
        ItemStack preview = getPreviewStack(host);
        return preview.isEmpty() ? ItemStack.EMPTY : preview.copyWithCount(1);
    }

    public static void setSelectedStack(ItemStack host, ItemStack selected) {
        FilterContents contents = get(host);
        set(host, contents.withSlotStack(contents.selectedSlot(), selected));
    }

    public static void setSlotStack(ItemStack host, int index, ItemStack stack) {
        FilterContents contents = get(host);
        set(host, contents.withSlotStack(index, stack));
    }

    public static ItemStack resolveUseRemainder(ItemStack consumed, int countBefore, boolean infiniteMaterials) {
        UseRemainder useRemainder = consumed.get(DataComponents.USE_REMAINDER);
        if (useRemainder == null) {
            return ItemStack.EMPTY;
        }
        ItemStack[] captured = new ItemStack[1];
        UseRemainder.OnExtraCreatedRemainder extraHandler = stack -> {
            if (stack.isEmpty()) {
                return;
            }
            if (captured[0] == null || captured[0].isEmpty()) {
                captured[0] = stack.copy();
            } else {
                captured[0].grow(stack.getCount());
            }
        };
        ItemStack converted = useRemainder.convertIntoRemainder(ItemStack.EMPTY, countBefore, infiniteMaterials, extraHandler);
        if (!converted.isEmpty()) {
            if (captured[0] == null || captured[0].isEmpty()) {
                return converted;
            }
            captured[0].grow(converted.getCount());
            return captured[0];
        }
        return captured[0] != null ? captured[0] : ItemStack.EMPTY;
    }

    public static void insertRemainderOrDrop(ItemStack host, int excludeIndex, ItemStack remainder, Player player) {
        if (remainder.isEmpty()) {
            return;
        }
        FilterContents contents = get(host);
        int max = contents.maxStackSize();
        ItemStack leftover = remainder.copy();

        for (int i = 0; i < contents.slotCount() && !leftover.isEmpty(); i++) {
            if (i == excludeIndex) {
                continue;
            }
            ItemStack inSlot = contents.stackInSlot(i);
            if (inSlot.isEmpty()) {
                int move = Math.min(leftover.getCount(), max);
                contents = contents.withSlotStack(i, leftover.copyWithCount(move));
                leftover.shrink(move);
            } else if (ItemStack.isSameItemSameComponents(inSlot, leftover)) {
                int space = max - inSlot.getCount();
                if (space > 0) {
                    int move = Math.min(space, leftover.getCount());
                    contents = contents.withSlotStack(i, inSlot.copyWithCount(inSlot.getCount() + move));
                    leftover.shrink(move);
                }
            }
        }

        set(host, contents);
        if (!leftover.isEmpty()) {
            player.drop(leftover, false);
        }
    }

    public static boolean canStore(ItemStack host, ItemStack candidate) {
        return !candidate.isEmpty() && !FilterRegistry.isFilterItem(candidate);
    }

    public static int absorb(ItemStack host, ItemStack incoming) {
        if (!canStore(host, incoming) || incoming.isEmpty()) {
            return incoming.getCount();
        }

        FilterProfile profile = profile(host);
        FilterContents contents = get(host);
        ItemResource resource = ItemResource.of(incoming);
        int remaining = incoming.getCount();
        int max = contents.maxStackSize();

        if (profile != null && profile.isBasic()) {
            Slot slot = contents.slot(0);
            if (!slot.isEmpty() && !slot.resource().equals(resource)) {
                return remaining;
            }
            int current = slot.isEmpty() ? 0 : slot.count();
            int move = Math.min(remaining, Math.max(0, max - current));
            if (move > 0) {
                set(host, contents.withSlot(0, resource, current + move));
            }
            return remaining - move;
        }

        boolean hadMatch = false;
        for (int i = 0; i < contents.slotCount() && remaining > 0; i++) {
            Slot slot = contents.slot(i);
            if (slot.isEmpty() || !slot.resource().equals(resource)) {
                continue;
            }
            hadMatch = true;
            if (slot.count() >= max) {
                continue;
            }
            int moved = Math.min(max - slot.count(), remaining);
            if (moved > 0) {
                contents = contents.withSlot(i, resource, slot.count() + moved);
                remaining -= moved;
            }
        }
        if (!hadMatch) {
            return incoming.getCount();
        }
        set(host, contents);
        return remaining;
    }

    public static boolean matchesAnySlot(ItemStack host, ItemResource resource) {
        if (resource.isEmpty() || !FilterRegistry.isFilterItem(host)) {
            return false;
        }
        FilterContents contents = get(host);
        for (int i = 0; i < contents.slotCount(); i++) {
            Slot slot = contents.slot(i);
            if (!slot.isEmpty() && slot.resource().equals(resource)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Single-pass pickup absorb: tries every cached filter slot, absorbs matching stacks,
     * returns true if any filter had a matching slot (caller should void the pickup).
     */
    public static boolean tryVoidPickup(Player player, int[] filterSlotIndices, ItemStack picked) {
        if (picked.isEmpty() || filterSlotIndices.length == 0) {
            return false;
        }
        ItemResource resource = ItemResource.of(picked);
        Inventory inventory = player.getInventory();
        int remaining = picked.getCount();
        boolean matched = false;

        for (int slotIndex : filterSlotIndices) {
            if (remaining <= 0) {
                break;
            }
            ItemStack host = inventory.getItem(slotIndex);
            if (!matchesAnySlot(host, resource)) {
                continue;
            }
            matched = true;
            ItemStack probe = picked.copyWithCount(remaining);
            remaining = absorb(host, probe);
            inventory.setItem(slotIndex, host);
        }
        return matched;
    }

    public static boolean hasMatchingFilter(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemResource resource = ItemResource.of(stack);
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack host = inventory.getItem(i);
            if (FilterRegistry.isFilterItem(host) && matchesAnySlot(host, resource)) {
                return true;
            }
        }
        return false;
    }

    public static int findMatchingSlot(ItemStack host, ItemStack filter) {
        FilterContents contents = get(host);
        for (int i = 0; i < contents.slotCount(); i++) {
            if (contents.slot(i).matches(filter)) {
                return i;
            }
        }
        return -1;
    }

    public static FilterContents gather(FilterContents contents) {
        int max = contents.maxStackSize();
        int count = contents.slotCount();
        List<Slot> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(contents.slot(i));
        }

        for (int i = 0; i < count; i++) {
            Slot slotI = slots.get(i);
            if (slotI.isEmpty()) {
                continue;
            }
            for (int j = 0; j < i; j++) {
                Slot slotJ = slots.get(j);
                if (slotJ.isEmpty() || !slotJ.resource().equals(slotI.resource())) {
                    continue;
                }
                int space = max - slotJ.count();
                if (space <= 0) {
                    continue;
                }
                int move = Math.min(space, slotI.count());
                slots.set(j, Slot.of(slotJ.resource(), slotJ.count() + move));
                slotI = Slot.of(slotI.resource(), slotI.count() - move);
                slots.set(i, slotI);
                if (slotI.isEmpty()) {
                    break;
                }
            }
        }

        List<Slot> compacted = new ArrayList<>(count);
        for (Slot slot : slots) {
            if (!slot.isEmpty()) {
                compacted.add(slot);
            }
        }
        while (compacted.size() < count) {
            compacted.add(Slot.EMPTY);
        }

        return new FilterContents(
                compacted,
                contents.maxStackSize(),
                contents.selectedSlot(),
                contents.page(),
                contents.color()
        );
    }

    public static boolean wouldGatherChange(FilterContents contents) {
        return !gather(contents).slots().equals(contents.slots());
    }

    public static int cycleNonEmptySlot(ItemStack host, int direction) {
        if (direction == 0) {
            return get(host).selectedSlot();
        }
        FilterContents contents = get(host);
        int start = contents.selectedSlot();
        int size = contents.slotCount();
        int step = direction > 0 ? 1 : -1;
        for (int offset = 1; offset <= size; offset++) {
            int index = Math.floorMod(start + offset * step, size);
            if (!contents.slot(index).isEmpty()) {
                set(host, contents.withSelectedSlot(index));
                return index;
            }
        }
        return start;
    }
}
