package com.dopa.randomutilities.solarpanel.menu;

import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.solarfurnace.SolarPower;
import com.dopa.randomutilities.solarpanel.SolarPanelControllerBlockEntity;
import com.dopa.randomutilities.solarpanel.config.SolarPanelConfig;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SolarPanelControllerMenu extends AbstractContainerMenu {
    public static final int TAB_Y_BIAS = 0;

    public static final int DATA_REDSTONE = 0;
    public static final int DATA_ENERGY_STORED = 1;
    public static final int DATA_ENERGY_CAPACITY = 2;
    public static final int DATA_ENERGY_USAGE = 3;
    public static final int DATA_ENERGY_MAX_OUT = 4;
    public static final int DATA_LINKED = 5;
    public static final int DATA_PEAK_FE = 6;
    public static final int DATA_SOLAR_PERMILLE = 7;
    public static final int DATA_SOLAR_STATUS = 8;
    public static final int DATA_MAX_RANGE = 9;
    public static final int DATA_SIZE = 10;

    public static final int ENERGY_BAR_X = 10;
    public static final int ENERGY_BAR_Y = 7;
    public static final int ENERGY_BAR_W = 11;
    public static final int ENERGY_BAR_H = 62;
    public static final int PLAYER_INV_Y = 84;

    private final SolarPanelControllerBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int playerInvStart;

    public SolarPanelControllerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public SolarPanelControllerMenu(int containerId, Inventory playerInv, SolarPanelControllerBlockEntity be) {
        super(ModMenus.SOLAR_PANEL_CONTROLLER.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = new SimpleContainerData(DATA_SIZE);

        List<MachineUpgradeSlot> upgrades = new ArrayList<>();
        UpgradeInventory handler = be.upgrades();
        for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
            MachineUpgradeSlot slot = new MachineUpgradeSlot(handler, i, 0);
            this.addSlot(slot);
            upgrades.add(slot);
        }
        this.upgradeSlots = Collections.unmodifiableList(upgrades);
        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);
        syncData();
        this.addDataSlots(data);
    }

    private static SolarPanelControllerBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof SolarPanelControllerBlockEntity controller) {
            return controller;
        }
        throw new IllegalStateException("Missing solar panel controller at " + pos);
    }

    private void syncData() {
        data.set(DATA_REDSTONE, be.redstoneMode().ordinal());
        data.set(DATA_ENERGY_STORED, be.energy().stored());
        data.set(DATA_ENERGY_CAPACITY, be.energy().capacity());
        data.set(DATA_ENERGY_USAGE, be.energy().lastTickUsage());
        data.set(DATA_ENERGY_MAX_OUT, be.energy().maxExtractRate());
        data.set(DATA_LINKED, be.linkedPanels());
        data.set(DATA_PEAK_FE, be.peakFePerTick());
        data.set(DATA_SOLAR_PERMILLE, be.solarPermille());
        data.set(DATA_SOLAR_STATUS, be.lastStatus().ordinal());
        data.set(DATA_MAX_RANGE, SolarPanelConfig.maxRange());
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public SolarPanelControllerBlockEntity blockEntity() {
        return be;
    }

    public UpgradeInventory upgrades() {
        return be.upgrades();
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

    public void setRedstoneMode(RedstoneMode mode) {
        be.setRedstoneMode(mode);
        syncData();
        broadcastChanges();
    }

    public int energyStored() {
        return data.get(DATA_ENERGY_STORED);
    }

    public int energyCapacity() {
        return data.get(DATA_ENERGY_CAPACITY);
    }

    public int energyUsage() {
        return data.get(DATA_ENERGY_USAGE);
    }

    public int energyMaxOut() {
        return data.get(DATA_ENERGY_MAX_OUT);
    }

    public int linkedPanels() {
        return data.get(DATA_LINKED);
    }

    public int peakFePerTick() {
        return data.get(DATA_PEAK_FE);
    }

    public int maxRange() {
        return data.get(DATA_MAX_RANGE);
    }

    public float solarStrengthFraction() {
        if (solarStatus() != SolarPower.Status.WORKING) {
            return 0.0F;
        }
        return Mth.clamp(data.get(DATA_SOLAR_PERMILLE) / 1000.0F, 0.0F, 1.0F);
    }

    public SolarPower.Status solarStatus() {
        int ordinal = data.get(DATA_SOLAR_STATUS);
        SolarPower.Status[] values = SolarPower.Status.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SolarPower.Status.NO_SUN;
        }
        return values[ordinal];
    }

    @Nullable
    public Level machineLevel() {
        return be.getLevel();
    }

    public BlockPos machinePos() {
        return be.getBlockPos();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < playerInvStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (UpgradeInventory.isUpgradeItem(stack)) {
            if (!this.moveItemStackTo(stack, 0, playerInvStart, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < playerInvStart + 27) {
            if (!this.moveItemStackTo(stack, playerInvStart + 27, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, playerInvStart, playerInvStart + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, be.getBlockState().getBlock());
    }
}
