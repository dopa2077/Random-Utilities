package com.dopa.randomutilities.trashcan;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.function.Predicate;

/**
 * Slot that clamps to normal stack size (voiding excess on set) and reports inflated
 * capacity so same-type overflow inserts can void like basic /dev/null.
 */
final class TrashCanSlot extends ResourceHandlerSlot {
    private final ItemStacksResourceHandler handler;
    private final Predicate<ItemStack> allowPlace;

    TrashCanSlot(TrashCanBlockEntity blockEntity, int x, int y, Predicate<ItemStack> allowPlace) {
        super(blockEntity.itemHandler(), blockEntity.itemHandler()::set, 0, x, y);
        this.handler = blockEntity.itemHandler();
        this.allowPlace = allowPlace;
    }

    private int realCapacity(ItemStack stack) {
        if (stack.isEmpty()) {
            ItemStack current = getItem();
            return current.isEmpty() ? 64 : Math.min(64, current.getMaxStackSize());
        }
        return Math.min(64, stack.getMaxStackSize());
    }

    @Override
    protected void setStackCopy(ItemStack stack) {
        if (stack.isEmpty()) {
            handler.set(0, ItemResource.EMPTY, 0);
            return;
        }
        if (!allowPlace.test(stack)) {
            return;
        }
        int cap = realCapacity(stack);
        ItemStack clamped = stack.copyWithCount(Math.min(stack.getCount(), cap));
        handler.set(0, ItemResource.of(clamped), clamped.getCount());
    }

    @Override
    public int getMaxStackSize() {
        return getMaxStackSize(ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        ItemStack current = getItem();
        int real = realCapacity(stack.isEmpty() ? current : stack);
        int currentCount = current.getCount();
        int itemMax = stack.isEmpty()
                ? (current.isEmpty() ? 64 : current.getMaxStackSize())
                : stack.getMaxStackSize();
        // Inflate when at/over soft cap so safeInsert can accept (and void) overflow.
        if (currentCount >= real) {
            return currentCount + Math.max(1, itemMax);
        }
        return real;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && allowPlace.test(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return !getItem().isEmpty();
    }
}
