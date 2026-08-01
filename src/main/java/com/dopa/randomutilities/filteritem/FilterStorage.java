package com.dopa.randomutilities.filteritem;

import com.dopa.randomutilities.filteritem.FilterContents.Slot;
import com.dopa.randomutilities.registry.ModDataComponents;

import java.util.List;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        if (profile.isBasic()) {
            return stored;
        }
        FilterContents contents = stored.ensureMinimum(profile.minSlots());
        if (contents.slotCount() != stored.slotCount()) {
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
            contents = new FilterContents(
                    contents.slots().isEmpty() ? List.of(Slot.EMPTY) : List.of(contents.slot(0)),
                    profile.fixedMaxStack() > 0 ? profile.fixedMaxStack() : contents.maxStackSize(),
                    0, 0, contents.color()
            );
        } else {
            contents = contents.ensureMinimum(profile.minSlots());
        }
        stack.set(ModDataComponents.FILTER_CONTENTS.get(), contents);
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
            int moved = Math.min(Math.max(0, max - current), remaining);
            if (moved > 0) {
                set(host, contents.withSlot(0, resource, current + moved));
                remaining -= moved;
            }
            return remaining;
        }

        boolean hadMatch = false;
        for (int i = 0; i < contents.slotCount() && remaining > 0; i++) {
            Slot slot = contents.slot(i);
            if (slot.isEmpty() || !slot.resource().equals(resource)) {
                continue;
            }
            hadMatch = true;
            int moved = Math.min(Math.max(0, max - slot.count()), remaining);
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

    public static boolean hasMatchingFilter(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack host = inventory.getItem(i);
            if (!FilterRegistry.isFilterItem(host)) {
                continue;
            }
            FilterContents contents = get(host);
            for (int slot = 0; slot < contents.slotCount(); slot++) {
                if (contents.slot(slot).matches(stack)) {
                    return true;
                }
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
