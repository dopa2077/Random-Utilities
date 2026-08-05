package com.dopa.randomutilities.minichest;

import com.dopa.randomutilities.minichest.MiniChestBlockEntity;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MiniChestMenu extends AbstractContainerMenu {
    /** Centered slot above a standard player inventory (chest-style chrome). */
    public static final int CHEST_SLOT_X = 80;
    public static final int CHEST_SLOT_Y = 18;
    public static final int PLAYER_INV_Y = 49;

    private final MiniChestBlockEntity chest;
    private final ContainerLevelAccess access;

    public MiniChestMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public MiniChestMenu(int containerId, Inventory playerInv, MiniChestBlockEntity chest) {
        super(ModMenus.MINI_CHEST.get(), containerId);
        this.chest = chest;
        this.access = ContainerLevelAccess.create(chest.getLevel(), chest.getBlockPos());
        this.addSlot(new Slot(chest, 0, CHEST_SLOT_X, CHEST_SLOT_Y));
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);
        chest.startOpen(playerInv.player);
    }

    private static MiniChestBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof MiniChestBlockEntity miniChest) {
            return miniChest;
        }
        throw new IllegalStateException("Missing mini chest at " + pos);
    }

    public Container getContainer() {
        return chest;
    }

    public MiniChestBlockEntity blockEntity() {
        return chest;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.chest.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.MINI_CHEST.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
