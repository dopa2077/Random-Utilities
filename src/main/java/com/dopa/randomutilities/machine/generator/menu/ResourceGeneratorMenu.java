package com.dopa.randomutilities.machine.generator.menu;

import com.dopa.randomutilities.util.PanelLayout;

import com.dopa.randomutilities.machine.generator.ResourceGeneratorBlockEntity;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.filter.menu.UpgradeSlot;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.core.BlockPos;
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

public class ResourceGeneratorMenu extends AbstractContainerMenu {
    public static final int TAB_Y_BIAS = 0;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_LOCKED = 2;
    public static final int DATA_REDSTONE = 3;
    public static final int DATA_HAS_MATCH = 4;
    public static final int DATA_MISSING_FLAGS = 5;
    public static final int DATA_SUPPORTS_LOCK = 6;
    public static final int DATA_SIZE = 7;

    private final ResourceGeneratorBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int playerInvStart;
    private final boolean upgradesEnabled;

    public ResourceGeneratorMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public ResourceGeneratorMenu(int containerId, Inventory playerInv, ResourceGeneratorBlockEntity be) {
        super(ModMenus.RESOURCE_GENERATOR.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = new SimpleContainerData(DATA_SIZE);
        this.upgradesEnabled = UpgradeConfig.upgradesEnabled(be.type());

        List<MachineUpgradeSlot> upgrades = new ArrayList<>();
        if (upgradesEnabled) {
            UpgradeInventory handler = be.upgrades();
            // Upgrade panel is RIGHT_TOP; UpgradeSlot.gridOriginY assumes BELOW_TAB_Y.
            int upgradeSlotYBias = TAB_Y_BIAS - PanelLayout.TAB_SIZE;
            for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
                MachineUpgradeSlot slot = new MachineUpgradeSlot(handler, i, upgradeSlotYBias);
                this.addSlot(slot);
                upgrades.add(slot);
            }
            this.playerInvStart = UpgradeConfig.UPGRADE_SLOT_COUNT;
        } else {
            this.playerInvStart = 0;
        }
        this.upgradeSlots = Collections.unmodifiableList(upgrades);

        this.addStandardInventorySlots(playerInv, 8, 84);
        syncData();
        this.addDataSlots(data);
    }

    private static ResourceGeneratorBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof ResourceGeneratorBlockEntity generator) {
            return generator;
        }
        throw new IllegalStateException("Missing resource generator at " + pos);
    }

    private void syncData() {
        data.set(DATA_PROGRESS, be.tickProgress());
        data.set(DATA_MAX_PROGRESS, be.effectiveTicks());
        data.set(DATA_LOCKED, be.isOutputLocked() ? 1 : 0);
        data.set(DATA_REDSTONE, be.redstoneMode().ordinal());
        data.set(DATA_HAS_MATCH, be.hasActiveMatch() ? 1 : 0);
        data.set(DATA_MISSING_FLAGS, be.getLevel() == null ? 0 : be.missingInputFlags(be.getLevel()));
        data.set(DATA_SUPPORTS_LOCK, be.supportsLockOutput() ? 1 : 0);
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public ResourceGeneratorBlockEntity blockEntity() {
        return be;
    }

    public boolean upgradesEnabled() {
        return upgradesEnabled;
    }

    public boolean supportsLockOutput() {
        return data.get(DATA_SUPPORTS_LOCK) != 0;
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return upgradesEnabled && index >= 0 && index < playerInvStart;
    }

    public float progressFraction() {
        int max = data.get(DATA_MAX_PROGRESS);
        if (max <= 0) {
            // Client data can lag; fall back to BE recipe ticks while matched.
            max = be.effectiveTicks();
        }
        if (max <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) data.get(DATA_PROGRESS) / (float) max);
    }

    public boolean isOutputLocked() {
        return data.get(DATA_LOCKED) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public int missingFlags() {
        return data.get(DATA_MISSING_FLAGS);
    }

    public boolean isGhostMissing(int ghostSlot) {
        return (missingFlags() & (1 << ghostSlot)) != 0;
    }

    public ItemStack generatorIcon() {
        return new ItemStack(be.getBlockState().getBlock().asItem());
    }

    public ItemStack outputIcon() {
        return be.displayResultStack();
    }

    public ItemStack ghostSideStack(int orderedIndex) {
        return be.ghostSideStack(orderedIndex);
    }

    public ItemStack ghostBelowStack() {
        return be.ghostBelowStack();
    }

    public void setOutputLocked(boolean locked) {
        be.setOutputLocked(locked);
        syncData();
        broadcastChanges();
    }

    public void setRedstoneMode(RedstoneMode mode) {
        be.setRedstoneMode(mode);
        syncData();
        broadcastChanges();
    }

    public GeneratorType generatorType() {
        return be.type();
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
        } else if (upgradesEnabled) {
            if (!UpgradeInventory.isSharedMachineUpgrade(stack)
                    || !this.moveItemStackTo(stack, 0, playerInvStart, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
