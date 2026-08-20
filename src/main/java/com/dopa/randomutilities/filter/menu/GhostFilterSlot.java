package com.dopa.randomutilities.filter.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.world.inventory.StackCopySlot;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Fake ghost filter slot: insert copies count 1 without consuming the cursor;
 * remove clears the slot and gives nothing back.
 */
public final class GhostFilterSlot extends StackCopySlot {
    private final GhostFilterHandler handler;
    private final int handlerIndex;
    private final Predicate<ItemStack> mayPlace;

    public GhostFilterSlot(GhostFilterHandler handler, int handlerIndex, int x, int y) {
        this(handler, handlerIndex, x, y, stack -> !stack.isEmpty());
    }

    public GhostFilterSlot(
            GhostFilterHandler handler,
            int handlerIndex,
            int x,
            int y,
            Predicate<ItemStack> mayPlace
    ) {
        super(handlerIndex, x, y);
        this.handler = handler;
        this.handlerIndex = handlerIndex;
        this.mayPlace = mayPlace;
    }

    @Override
    protected ItemStack getStackCopy() {
        return handler.getResource(handlerIndex).toStack(handler.getAmountAsInt(handlerIndex));
    }

    @Override
    protected void setStackCopy(ItemStack stack) {
        if (stack.isEmpty()) {
            handler.set(handlerIndex, ItemResource.EMPTY, 0);
        } else {
            handler.set(handlerIndex, ItemResource.of(stack), 1);
        }
    }

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return mayPlace.test(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return !getItem().isEmpty();
    }

    @Override
    public ItemStack safeInsert(ItemStack inputStack, int inputAmount) {
        if (!inputStack.isEmpty() && mayPlace(inputStack)) {
            set(inputStack.copyWithCount(1));
        }
        return inputStack;
    }

    @Override
    public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
        if (!mayPickup(player)) {
            return Optional.empty();
        }
        set(ItemStack.EMPTY);
        return Optional.empty();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
