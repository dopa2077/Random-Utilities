package com.dopa.randomutilities.logistics.transfer.menu;

import com.dopa.randomutilities.core.gui.panel.PanelLayout;
import com.dopa.randomutilities.core.machine.RedstoneMode;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.core.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.logistics.transfer.HeadKind;
import com.dopa.randomutilities.logistics.transfer.TransferNodeBlockEntity;
import com.dopa.randomutilities.logistics.transfer.TransferNodeUpgradeInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransferEnergyMenu extends AbstractContainerMenu {
    public static final int TAB_Y_BIAS = 0;
    public static final int DATA_REDSTONE = 0;
    public static final int DATA_LAST_ENERGY = 1;
    public static final int DATA_ENERGY_RATE = 2;
    public static final int DATA_SIZE = 3;
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 155;
    public static final int PLAYER_INV_Y = 74;

    private final TransferNodeBlockEntity blockEntity;
    private final Direction face;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int playerInvStart;

    public TransferEnergyMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolve(playerInv, buf.readBlockPos()), Direction.from3DDataValue(buf.readByte()));
    }

    public TransferEnergyMenu(
            int containerId,
            Inventory playerInv,
            TransferNodeBlockEntity blockEntity,
            Direction face
    ) {
        super(ModMenus.TRANSFER_NODE_ENERGY.get(), containerId);
        this.blockEntity = blockEntity;
        this.face = face;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        TransferNodeBlockEntity.Head head = blockEntity.head(face);
        List<MachineUpgradeSlot> upgrades = new ArrayList<>();
        int upgradeSlotYBias = TAB_Y_BIAS - PanelLayout.TAB_SIZE;
        for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
            MachineUpgradeSlot slot = new MachineUpgradeSlot(head.upgrades(), i, upgradeSlotYBias);
            this.addSlot(slot);
            upgrades.add(slot);
        }
        this.upgradeSlots = Collections.unmodifiableList(upgrades);
        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);
        this.data = new SimpleContainerData(DATA_SIZE);
        syncData();
        this.addDataSlots(data);
    }

    private static TransferNodeBlockEntity resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof TransferNodeBlockEntity node) {
            return node;
        }
        throw new IllegalStateException("Missing transfer node at " + pos);
    }

    private void syncData() {
        TransferNodeBlockEntity.Head head = blockEntity.head(face);
        data.set(DATA_REDSTONE, head.redstoneMode().ordinal());
        data.set(DATA_LAST_ENERGY, head.lastEnergyPulled());
        data.set(DATA_ENERGY_RATE, head.energyPullRate());
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public Direction face() {
        return face;
    }

    public TransferNodeUpgradeInventory upgrades() {
        return blockEntity.head(face).upgrades();
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return index >= 0 && index < playerInvStart;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public int lastEnergyPulled() {
        return data.get(DATA_LAST_ENERGY);
    }

    public int energyPullRate() {
        return data.get(DATA_ENERGY_RATE);
    }

    public void setRedstoneMode(RedstoneMode mode) {
        blockEntity.head(face).setRedstoneMode(mode);
        blockEntity.setChanged();
        syncData();
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TRANSFER_NODE.get()) && blockEntity.hasHead(face);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index < playerInvStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            ItemStack remaining = stack.copy();
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return remaining;
        }
        if (upgrades().accepts(stack)
                && this.moveItemStackTo(stack, 0, playerInvStart, false)) {
            ItemStack remaining = stack.copy();
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return remaining;
        }
        return ItemStack.EMPTY;
    }
}
