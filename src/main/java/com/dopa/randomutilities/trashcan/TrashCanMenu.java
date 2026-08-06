package com.dopa.randomutilities.trashcan;

import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TrashCanMenu extends AbstractContainerMenu {
    public static final int CHEST_SLOT_X = 80;
    public static final int CHEST_SLOT_Y = 18;
    public static final int PLAYER_INV_Y = 49;

    private final TrashCanBlockEntity trashCan;
    private final ContainerLevelAccess access;

    public TrashCanMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public TrashCanMenu(int containerId, Inventory playerInv, TrashCanBlockEntity trashCan) {
        super(ModMenus.TRASH_CAN.get(), containerId);
        this.trashCan = trashCan;
        this.access = ContainerLevelAccess.create(trashCan.getLevel(), trashCan.getBlockPos());
        this.addSlot(new TrashCanSlot(trashCan.itemHandler(), CHEST_SLOT_X, CHEST_SLOT_Y));
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);
    }

    private static TrashCanBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof TrashCanBlockEntity trashCan) {
            return trashCan;
        }
        throw new IllegalStateException("Missing trash can at " + pos);
    }

    public TrashCanBlockEntity blockEntity() {
        return trashCan;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (slotId == 0
                && containerInput == ContainerInput.PICKUP
                && (button == 0 || button == 1)
                && tryVoidReplaceClick(button)) {
            return;
        }
        super.clicked(slotId, button, containerInput, player);
    }

    /** Different item on occupied slot: void previous, then insert from cursor (no swap). */
    private boolean tryVoidReplaceClick(int button) {
        Slot slot = this.slots.getFirst();
        ItemStack carried = this.getCarried();
        ItemStack slotStack = slot.getItem();
        if (carried.isEmpty() || slotStack.isEmpty()) {
            return false;
        }
        if (ItemStack.isSameItemSameComponents(slotStack, carried)) {
            return false;
        }
        if (!slot.mayPlace(carried)) {
            return false;
        }
        slot.setByPlayer(ItemStack.EMPTY);
        int amount = button == 0 ? carried.getCount() : 1;
        this.setCarried(slot.safeInsert(carried, amount));
        slot.setChanged();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.TRASH_CAN.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            ItemStack toInsert = stack.copy();
            Slot trashSlot = this.slots.getFirst();
            ItemStack before = trashSlot.getItem().copy();
            int countBefore = toInsert.getCount();
            trashSlot.safeInsert(toInsert, toInsert.getCount());
            int moved = countBefore - toInsert.getCount();
            if (moved <= 0 && ItemStack.matches(before, trashSlot.getItem())) {
                // Type change void-replace path for shift-click
                if (!before.isEmpty() && !ItemStack.isSameItemSameComponents(before, stack)) {
                    trashSlot.setByPlayer(ItemStack.EMPTY);
                    trashSlot.safeInsert(toInsert, toInsert.getCount());
                    moved = countBefore - toInsert.getCount();
                }
            }
            if (moved <= 0 && toInsert.getCount() == countBefore) {
                return ItemStack.EMPTY;
            }
            stack.setCount(toInsert.getCount());
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
}
