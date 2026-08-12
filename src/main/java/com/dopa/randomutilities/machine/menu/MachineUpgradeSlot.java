package com.dopa.randomutilities.machine.menu;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.util.PanelLayout;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.world.inventory.StackCopySlot;

public class MachineUpgradeSlot extends StackCopySlot {
    public static final int COLS = 3;
    public static final int ROWS = 2;
    public static final int TITLE_GAP = 18;

    private final UpgradeInventory handler;
    private final int handlerIndex;
    private final int layoutYBias;
    private boolean active;

    public MachineUpgradeSlot(UpgradeInventory handler, int index, int layoutYBias) {
        super(index, slotX(index), slotY(index) + layoutYBias);
        this.handler = handler;
        this.handlerIndex = index;
        this.layoutYBias = layoutYBias;
    }

    public static int panelWidth() {
        return Math.max(PanelLayout.CONTENT_PAD * 2 + COLS * 18, 100);
    }

    public static int gridWidth() {
        return COLS * 18;
    }

    public static int bodyOriginX() {
        return PanelLayout.GUI_WIDTH - 1;
    }

    public static int gridOriginX() {
        return bodyOriginX() + (panelWidth() - gridWidth()) / 2;
    }

    public static int gridOriginY() {
        return PanelLayout.BELOW_TAB_Y + PanelLayout.CONTENT_PAD + TITLE_GAP;
    }

    public static int slotX(int index) {
        return gridOriginX() + (index % COLS) * 18;
    }

    public static int slotY(int index) {
        return gridOriginY() + (index / COLS) * 18;
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
