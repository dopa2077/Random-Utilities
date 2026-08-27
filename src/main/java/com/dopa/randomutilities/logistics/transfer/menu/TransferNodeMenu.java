package com.dopa.randomutilities.logistics.transfer.menu;

import com.dopa.randomutilities.core.filter.menu.GhostFilterHandler;
import com.dopa.randomutilities.core.filter.menu.GhostFilterMenu;
import com.dopa.randomutilities.core.filter.menu.GhostFilterSlot;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.logistics.transfer.HeadKind;
import com.dopa.randomutilities.logistics.transfer.TransferNodeBlock;
import com.dopa.randomutilities.logistics.transfer.TransferNodeBlockEntity;
import com.dopa.randomutilities.logistics.transfer.TransferNodeUpgradeInventory;
import com.dopa.randomutilities.core.machine.RedstoneMode;
import com.dopa.randomutilities.core.machine.UpgradeInventory;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.core.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.core.gui.panel.PanelLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.world.inventory.StackCopySlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TransferNodeMenu extends AbstractContainerMenu implements GhostFilterMenu {
    public static final int TAB_Y_BIAS = 0;
    public static final int DATA_WHITELIST_MODE = 0;
    public static final int DATA_REDSTONE = 1;
    public static final int DATA_SIZE = 2;

    public static final int FILTER_SLOT_COUNT = TransferNodeBlockEntity.FILTER_SLOT_COUNT;
    public static final int DISPLAY_SLOT = UpgradeConfig.UPGRADE_SLOT_COUNT;
    public static final int FILTER_START = DISPLAY_SLOT + 1;
    public static final int IMAGE_WIDTH = 176;
    public static final int DISPLAY_SLOT_X = 80;
    public static final int DISPLAY_SLOT_Y = 20;
    public static final int FILTER_ICON_X = 8;
    public static final int FILTER_SLOT_X = 26;
    public static final int FILTER_SLOT_Y = 43;
    public static final int PLAYER_INV_Y = 74;
    public static final int IMAGE_HEIGHT = 155;

    private final TransferNodeBlockEntity blockEntity;
    private final Direction face;
    private final ContainerLevelAccess access;
    private final GhostFilterHandler filterHandler;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int playerInvStart;

    public TransferNodeMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()),
                Direction.from3DDataValue(buf.readByte()));
    }

    public TransferNodeMenu(int containerId, Inventory playerInv, TransferNodeBlockEntity blockEntity) {
        this(containerId, playerInv, blockEntity, Direction.NORTH);
    }

    public TransferNodeMenu(
            int containerId,
            Inventory playerInv,
            TransferNodeBlockEntity blockEntity,
            Direction face
    ) {
        super(ModMenus.TRANSFER_NODE.get(), containerId);
        this.blockEntity = blockEntity;
        this.face = face;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        TransferNodeBlockEntity.Head head = blockEntity.head(face);
        List<MachineUpgradeSlot> upgrades = new ArrayList<>();
        UpgradeInventory handler = head.upgrades();
        int upgradeSlotYBias = TAB_Y_BIAS - PanelLayout.TAB_SIZE;
        for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
            MachineUpgradeSlot slot = new MachineUpgradeSlot(handler, i, upgradeSlotYBias);
            this.addSlot(slot);
            upgrades.add(slot);
        }
        this.upgradeSlots = Collections.unmodifiableList(upgrades);

        NonNullList<ItemStack> stacks = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            stacks.set(i, head.filterSlots().get(i));
        }
        this.filterHandler = new GhostFilterHandler(stacks);
        this.filterHandler.setOnChanged(() -> {
            saveFilters();
            blockEntity.setChanged();
        });

        this.addSlot(new DisplaySlot(blockEntity.head(face), DISPLAY_SLOT_X, DISPLAY_SLOT_Y));
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            this.addSlot(new GhostFilterSlot(filterHandler, i, FILTER_SLOT_X + i * 18, FILTER_SLOT_Y));
        }

        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);

        this.data = new SimpleContainerData(DATA_SIZE);
        syncData();
        this.addDataSlots(data);
    }

    private static TransferNodeBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof TransferNodeBlockEntity node) {
            return node;
        }
        throw new IllegalStateException("Missing transfer node at " + pos);
    }

    private void saveFilters() {
        TransferNodeBlockEntity.Head head = blockEntity.head(face);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            head.setFilterSlot(i, filterHandler.getResource(i).toStack(filterHandler.getAmountAsInt(i)));
        }
    }

    private void syncData() {
        data.set(DATA_WHITELIST_MODE, blockEntity.head(face).whitelistMode() ? 1 : 0);
        data.set(DATA_REDSTONE, blockEntity.head(face).redstoneMode().ordinal());
    }

    @Override
    public void broadcastChanges() {
        syncData();
        if (slots.get(DISPLAY_SLOT) instanceof DisplaySlot display) {
            display.syncFromHead();
        }
        super.broadcastChanges();
    }

    public HeadKind kind() {
        return blockEntity.head(face).kind();
    }

    public TransferNodeBlockEntity blockEntity() {
        return blockEntity;
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
        return index >= 0 && index < DISPLAY_SLOT;
    }

    public boolean isDisplaySlot(Slot slot) {
        return slot != null && slot.index == DISPLAY_SLOT;
    }

    public boolean isWhitelistMode() {
        return data.get(DATA_WHITELIST_MODE) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public void setRedstoneMode(RedstoneMode mode) {
        blockEntity.head(face).setRedstoneMode(mode);
        blockEntity.setChanged();
        syncData();
        broadcastChanges();
    }

    public void setWhitelistMode(boolean whitelist) {
        blockEntity.head(face).setWhitelistMode(whitelist);
        blockEntity.setChanged();
        syncData();
        broadcastChanges();
    }

    public void setFilterSlot(int index, ItemStack stack) {
        blockEntity.head(face).setFilterSlot(index, stack);
        blockEntity.setChanged();
        if (index >= 0 && index < FILTER_SLOT_COUNT) {
            if (stack.isEmpty()) {
                filterHandler.set(index, ItemResource.EMPTY, 0);
            } else {
                filterHandler.set(index, ItemResource.of(stack), 1);
            }
        }
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TRANSFER_NODE.get())
                && blockEntity.hasHead(face);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index == DISPLAY_SLOT) {
            return ItemStack.EMPTY;
        }
        if (index < DISPLAY_SLOT) {
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
        if (index < playerInvStart) {
            slot.setByPlayer(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
        if (upgrades().accepts(stack)
                && this.moveItemStackTo(stack, 0, DISPLAY_SLOT, false)) {
            ItemStack remaining = stack.copy();
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return remaining;
        }
        if (upgrades().accepts(stack)) {
            return ItemStack.EMPTY;
        }
        for (int i = FILTER_START; i < playerInvStart; i++) {
            Slot filterSlot = this.slots.get(i);
            if (filterSlot.getItem().isEmpty()) {
                filterSlot.safeInsert(stack, 1);
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !slot.isFake();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot.isFake()) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveFilters();
    }

    @Override
    public int filterSlotCount() {
        return FILTER_SLOT_COUNT;
    }

    private static final class DisplaySlot extends StackCopySlot {
        private final TransferNodeBlockEntity.Head head;
        private ItemStack cached = ItemStack.EMPTY;

        DisplaySlot(TransferNodeBlockEntity.Head head, int x, int y) {
            super(DISPLAY_SLOT, x, y);
            this.head = head;
            syncFromHead();
        }

        void syncFromHead() {
            ItemStack display = head.transferredDisplay();
            cached = display.isEmpty() ? ItemStack.EMPTY : display.copyWithCount(1);
        }

        @Override
        protected ItemStack getStackCopy() {
            return cached.isEmpty() ? ItemStack.EMPTY : cached.copy();
        }

        @Override
        protected void setStackCopy(ItemStack stack) {
            cached = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        }

        @Override
        public boolean isFake() {
            return true;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public ItemStack safeInsert(ItemStack inputStack, int inputAmount) {
            return inputStack;
        }

        @Override
        public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
            return Optional.empty();
        }
    }
}
