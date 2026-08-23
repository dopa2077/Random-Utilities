package com.dopa.randomutilities.combustion.menu;

import com.dopa.randomutilities.combustion.CombustionGeneratorBlockEntity;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
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
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombustionGeneratorMenu extends AbstractContainerMenu {
    public static final int TAB_Y_BIAS = 0;

    public static final int DATA_BURN = 0;
    public static final int DATA_REDSTONE = 1;
    public static final int DATA_ENERGY_STORED = 2;
    public static final int DATA_ENERGY_CAPACITY = 3;
    public static final int DATA_ENERGY_USAGE = 4;
    public static final int DATA_ENERGY_MAX_OUT = 5;
    public static final int DATA_SIZE = 6;

    public static final int ENERGY_BAR_X = 10;
    public static final int ENERGY_BAR_Y = 7;
    public static final int ENERGY_BAR_W = 11;
    public static final int ENERGY_BAR_H = 62;

    public static final int FUEL_SLOT_X = 80;
    public static final int FUEL_SLOT_Y = 26;
    /** Matches the heat-gauge art in {@code basic_generator.png}. */
    public static final int BURN_X = 79;
    public static final int BURN_Y = 42;
    public static final int BURN_W = 14;
    public static final int BURN_H = 14;
    /** Lit overlay packed into the GUI texture at this UV. */
    public static final int BURN_TEX_U = 176;
    public static final int BURN_TEX_V = 0;

    public static final int PLAYER_INV_Y = 84;

    private final CombustionGeneratorBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int machineSlotStart;
    private final int playerInvStart;

    public CombustionGeneratorMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public CombustionGeneratorMenu(int containerId, Inventory playerInv, CombustionGeneratorBlockEntity be) {
        super(ModMenus.COMBUSTION_GENERATOR.get(), containerId);
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
        this.machineSlotStart = UpgradeConfig.UPGRADE_SLOT_COUNT;

        this.addSlot(new ResourceHandlerSlot(
                be.items(),
                be.items()::set,
                CombustionGeneratorBlockEntity.SLOT_FUEL,
                FUEL_SLOT_X,
                FUEL_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CombustionGeneratorBlockEntity.burnDuration(stack, playerInv.player.level()) > 0;
            }
        });
        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, PLAYER_INV_Y);
        syncData();
        this.addDataSlots(data);
    }

    private static CombustionGeneratorBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof CombustionGeneratorBlockEntity generator) {
            return generator;
        }
        throw new IllegalStateException("Missing combustion generator at " + pos);
    }

    private void syncData() {
        data.set(DATA_BURN, be.burnProgressSynced());
        data.set(DATA_REDSTONE, be.redstoneMode().ordinal());
        data.set(DATA_ENERGY_STORED, be.energy().stored());
        data.set(DATA_ENERGY_CAPACITY, be.energy().capacity());
        data.set(DATA_ENERGY_USAGE, be.energy().lastTickUsage());
        data.set(DATA_ENERGY_MAX_OUT, be.energy().maxExtractRate());
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public CombustionGeneratorBlockEntity blockEntity() {
        return be;
    }

    public UpgradeInventory upgrades() {
        return be.upgrades();
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return index >= 0 && index < machineSlotStart;
    }

    public float burnFraction() {
        return data.get(DATA_BURN) / 1000.0F;
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < machineSlotStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == machineSlotStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (CombustionGeneratorBlockEntity.burnDuration(stack, player.level()) > 0) {
                if (!this.moveItemStackTo(stack, machineSlotStart, machineSlotStart + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (UpgradeInventory.isUpgradeItem(stack)) {
                if (!this.moveItemStackTo(stack, 0, machineSlotStart, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < playerInvStart + 27) {
                if (!this.moveItemStackTo(stack, playerInvStart + 27, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, playerInvStart, playerInvStart + 27, false)) {
                return ItemStack.EMPTY;
            }
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
