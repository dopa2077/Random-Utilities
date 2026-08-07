package com.dopa.randomutilities.redstoneclock;

import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RedstoneClockMenu extends AbstractContainerMenu {
    public static final int DATA_INTERVAL = 0;
    public static final int DATA_PULSE = 1;
    public static final int DATA_REDSTONE = 2;
    public static final int DATA_SIZE = 3;

    public static final int PLAYER_INV_Y = 84;

    private final RedstoneClockBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public RedstoneClockMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public RedstoneClockMenu(int containerId, Inventory playerInv, RedstoneClockBlockEntity blockEntity) {
        super(ModMenus.REDSTONE_CLOCK.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);
        this.data = new SimpleContainerData(DATA_SIZE);
        syncData();
        this.addDataSlots(data);
    }

    private static RedstoneClockBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof RedstoneClockBlockEntity clock) {
            return clock;
        }
        throw new IllegalStateException("Missing redstone clock at " + pos);
    }

    private void syncData() {
        data.set(DATA_INTERVAL, blockEntity.interval());
        data.set(DATA_PULSE, blockEntity.pulseLength());
        data.set(DATA_REDSTONE, blockEntity.redstoneMode().ordinal());
    }

    public RedstoneClockBlockEntity blockEntity() {
        return blockEntity;
    }

    public int interval() {
        return data.get(DATA_INTERVAL);
    }

    public int pulseLength() {
        return data.get(DATA_PULSE);
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public void setInterval(int value) {
        blockEntity.setInterval(value);
        syncData();
        broadcastChanges();
    }

    public void setPulseLength(int value) {
        blockEntity.setPulseLength(value);
        syncData();
        broadcastChanges();
    }

    public void setRedstoneMode(RedstoneMode mode) {
        blockEntity.setRedstoneMode(mode);
        syncData();
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.REDSTONE_CLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
