package com.dopa.randomutilities.itemcollector.menu;

import com.dopa.randomutilities.filter.menu.GhostFilterHandler;
import com.dopa.randomutilities.filter.menu.GhostFilterMenu;
import com.dopa.randomutilities.filter.menu.GhostFilterSlot;
import com.dopa.randomutilities.itemcollector.ItemCollectorBlockEntity;
import com.dopa.randomutilities.itemcollector.ItemCollectorType;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.gui.panel.PanelLayout;

import net.minecraft.core.BlockPos;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemCollectorMenu extends AbstractContainerMenu implements GhostFilterMenu {
    public static final int DATA_RANGE_X = 0;
    public static final int DATA_RANGE_Y = 1;
    public static final int DATA_RANGE_Z = 2;
    public static final int DATA_PICKUP_DELAY = 3;
    public static final int DATA_PICKUP_BATCH = 4;
    public static final int DATA_WHITELIST_MODE = 5;
    public static final int DATA_REDSTONE = 6;
    public static final int DATA_OVERLAY_COLOR = 7;
    public static final int DATA_PARTICLES = 8;
    public static final int DATA_MAX_RANGE = 9;
    public static final int DATA_SIZE = 10;

    public static final int IMAGE_WIDTH = 176;
    public static final int FILTER_SLOT_Y = 20;
    public static final int PLAYER_INV_Y = 51;
    /** Advanced: icon column at 8, first filter slot at 26. */
    public static final int ADVANCED_ICON_X = 8;
    public static final int ADVANCED_FILTER_SLOT_X = 26;

    public static int iconX(ItemCollectorType type) {
        if (type == ItemCollectorType.BASIC) {
            int groupWidth = 18 + type.filterSlotCount() * 18;
            return (IMAGE_WIDTH - groupWidth) / 2;
        }
        return ADVANCED_ICON_X;
    }

    public static int filterSlotX(ItemCollectorType type) {
        return iconX(type) + 18;
    }

    private final ItemCollectorBlockEntity blockEntity;
    private final ItemCollectorType type;
    private final ContainerLevelAccess access;
    private final GhostFilterHandler filterHandler;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int filterStart;
    private final int playerInvStart;

    public ItemCollectorMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public ItemCollectorMenu(int containerId, Inventory playerInv, ItemCollectorBlockEntity blockEntity) {
        super(ModMenus.ITEM_COLLECTOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.type = blockEntity.collectorType();
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        List<MachineUpgradeSlot> upgrades = new ArrayList<>();
        UpgradeInventory handler = blockEntity.upgrades();
        // Upgrade panel is RIGHT_TOP; MachineUpgradeSlot.gridOriginY assumes BELOW_TAB_Y.
        int upgradeSlotYBias = -PanelLayout.TAB_SIZE;
        for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
            MachineUpgradeSlot slot = new MachineUpgradeSlot(handler, i, upgradeSlotYBias);
            this.addSlot(slot);
            upgrades.add(slot);
        }
        this.upgradeSlots = Collections.unmodifiableList(upgrades);

        this.filterStart = this.slots.size();
        NonNullList<ItemStack> stacks = NonNullList.withSize(type.filterSlotCount(), ItemStack.EMPTY);
        for (int i = 0; i < type.filterSlotCount(); i++) {
            stacks.set(i, blockEntity.filterSlots().get(i));
        }
        this.filterHandler = new GhostFilterHandler(stacks);
        this.filterHandler.setOnChanged(() -> {
            saveFilters();
            blockEntity.setChanged();
        });

        int slotCount = type.filterSlotCount();
        int slotX = filterSlotX(type);
        for (int i = 0; i < slotCount; i++) {
            this.addSlot(new GhostFilterSlot(filterHandler, i, slotX + i * 18, FILTER_SLOT_Y));
        }

        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);

        this.data = new SimpleContainerData(DATA_SIZE);
        syncData();
        this.addDataSlots(data);
    }

    private static ItemCollectorBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof ItemCollectorBlockEntity collector) {
            return collector;
        }
        throw new IllegalStateException("Missing item collector at " + pos);
    }

    private void saveFilters() {
        for (int i = 0; i < type.filterSlotCount(); i++) {
            blockEntity.setFilterSlot(i, filterHandler.getResource(i).toStack(filterHandler.getAmountAsInt(i)));
        }
    }

    private void syncData() {
        data.set(DATA_RANGE_X, blockEntity.rangeX());
        data.set(DATA_RANGE_Y, blockEntity.rangeY());
        data.set(DATA_RANGE_Z, blockEntity.rangeZ());
        data.set(DATA_PICKUP_DELAY, blockEntity.pickupDelay());
        data.set(DATA_PICKUP_BATCH, blockEntity.pickupBatch());
        data.set(DATA_WHITELIST_MODE, blockEntity.whitelistMode() ? 1 : 0);
        data.set(DATA_REDSTONE, blockEntity.redstoneMode().ordinal());
        data.set(DATA_OVERLAY_COLOR, blockEntity.overlayColor());
        data.set(DATA_PARTICLES, blockEntity.particlesEnabled() ? 1 : 0);
        data.set(DATA_MAX_RANGE, blockEntity.maxRange());
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return index >= 0 && index < filterStart;
    }

    public int filterSlotStart() {
        return filterStart;
    }

    public ItemCollectorBlockEntity blockEntity() {
        return blockEntity;
    }

    public ItemCollectorType collectorType() {
        return type;
    }

    public int getRangeX() {
        return data.get(DATA_RANGE_X);
    }

    public int getRangeY() {
        return data.get(DATA_RANGE_Y);
    }

    public int getRangeZ() {
        return data.get(DATA_RANGE_Z);
    }

    public int getPickupDelay() {
        return data.get(DATA_PICKUP_DELAY);
    }

    public int getPickupBatch() {
        return data.get(DATA_PICKUP_BATCH);
    }

    public boolean isWhitelistMode() {
        return data.get(DATA_WHITELIST_MODE) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public int getOverlayColor() {
        return data.get(DATA_OVERLAY_COLOR) & 0xFFFFFF;
    }

    public boolean isParticlesEnabled() {
        return data.get(DATA_PARTICLES) != 0;
    }

    public int maxRange() {
        return data.get(DATA_MAX_RANGE);
    }

    public void setRedstoneMode(RedstoneMode mode) {
        blockEntity.setRedstoneMode(mode);
        data.set(DATA_REDSTONE, blockEntity.redstoneMode().ordinal());
    }

    public void setRangeX(int value) {
        blockEntity.setRangeX(value);
        data.set(DATA_RANGE_X, blockEntity.rangeX());
    }

    public void setRangeY(int value) {
        blockEntity.setRangeY(value);
        data.set(DATA_RANGE_Y, blockEntity.rangeY());
    }

    public void setRangeZ(int value) {
        blockEntity.setRangeZ(value);
        data.set(DATA_RANGE_Z, blockEntity.rangeZ());
    }

    public void setPickupDelay(int value) {
        blockEntity.setPickupDelay(value);
        data.set(DATA_PICKUP_DELAY, blockEntity.pickupDelay());
    }

    public void setPickupBatch(int value) {
        blockEntity.setPickupBatch(value);
        data.set(DATA_PICKUP_BATCH, blockEntity.pickupBatch());
    }

    public void setWhitelistMode(boolean whitelist) {
        blockEntity.setWhitelistMode(whitelist);
        data.set(DATA_WHITELIST_MODE, blockEntity.whitelistMode() ? 1 : 0);
    }

    public void setOverlayColor(int color) {
        blockEntity.setOverlayColor(color);
        data.set(DATA_OVERLAY_COLOR, blockEntity.overlayColor());
    }

    public void setParticlesEnabled(boolean enabled) {
        blockEntity.setParticlesEnabled(enabled);
        data.set(DATA_PARTICLES, blockEntity.particlesEnabled() ? 1 : 0);
    }

    public void setFilterSlot(int index, ItemStack stack) {
        blockEntity.setFilterSlot(index, stack);
        if (index >= 0 && index < type.filterSlotCount()) {
            if (stack.isEmpty()) {
                filterHandler.set(index, ItemResource.EMPTY, 0);
            } else {
                filterHandler.set(index, ItemResource.of(stack), 1);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                type == ItemCollectorType.BASIC ? ModBlocks.BASIC_ITEM_COLLECTOR.get() : ModBlocks.ADVANCED_ITEM_COLLECTOR.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int filterSlots = type.filterSlotCount();
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index < filterStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return stack.copy();
        }
        if (index >= filterStart && index < playerInvStart) {
            slot.setByPlayer(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
        if (UpgradeInventory.isCollectorUpgrade(stack)
                && this.moveItemStackTo(stack, 0, filterStart, false)) {
            ItemStack remaining = stack.copy();
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return remaining;
        }
        if (UpgradeInventory.isCollectorUpgrade(stack)) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < filterSlots; i++) {
            Slot filterSlot = this.slots.get(filterStart + i);
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
        // Ghost filter slots must not participate in double-click gather.
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
        return collectorType().filterSlotCount();
    }
}
