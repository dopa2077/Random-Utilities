package com.dopa.randomutilities.machine.menu;

import com.dopa.randomutilities.filtersystem.menu.UpgradeSlot;
import com.dopa.randomutilities.machine.UpgradeInventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.world.inventory.StackCopySlot;

public class MachineUpgradeSlot extends StackCopySlot {
    private final UpgradeInventory handler;
    private final int handlerIndex;
    private final int layoutYBias;
    private boolean active;

    public MachineUpgradeSlot(UpgradeInventory handler, int index, int layoutYBias) {
        super(index, UpgradeSlot.slotX(index), UpgradeSlot.slotY(index) + layoutYBias);
        this.handler = handler;
        this.handlerIndex = index;
        this.layoutYBias = layoutYBias;
    }

    public int layoutYBias() {
        return layoutYBias;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    protected ItemStack getStackCopy() {
        return handler.getResource(handlerIndex).toStack(handler.getAmountAsInt(handlerIndex));
    }

    @Override
    protected void setStackCopy(ItemStack stack) {
        handler.set(handlerIndex, ItemResource.of(stack), stack.getCount());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && handler.isValid(handlerIndex, ItemResource.of(stack));
    }

    @Override
    public boolean mayPickup(Player player) {
        return !getItem().isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return Math.max(1, handler.getCapacityAsInt(handlerIndex, ItemResource.EMPTY));
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.max(0, handler.getCapacityAsInt(handlerIndex, ItemResource.of(stack)));
    }
}
