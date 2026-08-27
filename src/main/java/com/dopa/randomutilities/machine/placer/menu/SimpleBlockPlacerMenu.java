package com.dopa.randomutilities.machine.placer.menu;

import com.dopa.randomutilities.machine.placer.SimpleBlockPlacerBlockEntity;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class SimpleBlockPlacerMenu extends AbstractContainerMenu {
    public static final int TAB_Y_BIAS = 0;
    public static final int GRID_LEFT = 62;
    public static final int GRID_TOP = 17;
    public static final int SLOT_COUNT = SimpleBlockPlacerBlockEntity.SLOT_COUNT;

    private final SimpleBlockPlacerBlockEntity be;
    private final ContainerLevelAccess access;
    private final int playerInvStart;

    public SimpleBlockPlacerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public SimpleBlockPlacerMenu(int containerId, Inventory playerInv, SimpleBlockPlacerBlockEntity be) {
        super(ModMenus.SIMPLE_BLOCK_PLACER.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new ResourceHandlerSlot(
                        be.itemHandler(),
                        be.itemHandler()::set,
                        index,
                        GRID_LEFT + col * 18,
                        GRID_TOP + row * 18
                ));
            }
        }
        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, 84);
    }

    private static SimpleBlockPlacerBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof SimpleBlockPlacerBlockEntity placer) {
            return placer;
        }
        throw new IllegalStateException("Missing simple block placer at " + pos);
    }

    public SimpleBlockPlacerBlockEntity blockEntity() {
        return be;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, be.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack remaining = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return remaining;
        }
        ItemStack stack = slot.getItem();
        remaining = stack.copy();
        if (index < playerInvStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, playerInvStart, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return remaining;
    }
}
