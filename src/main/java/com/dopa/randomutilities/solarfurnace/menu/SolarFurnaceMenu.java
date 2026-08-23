package com.dopa.randomutilities.solarfurnace.menu;

import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.solarfurnace.SolarFurnaceBlockEntity;
import com.dopa.randomutilities.solarfurnace.SolarPower;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.gui.panel.PanelLayout;

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
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SolarFurnaceMenu extends AbstractContainerMenu {
    public static final int TAB_Y_BIAS = 0;

    public static final int DATA_COOK_PROGRESS = 0;
    public static final int DATA_COOK_TOTAL = 1;
    public static final int DATA_REDSTONE = 2;
    public static final int DATA_SOLAR_PERMILLE = 3;
    public static final int DATA_SOLAR_STATUS = 4;
    public static final int DATA_SIZE = 5;

    public static final int INPUT_SLOT_X = 56;
    public static final int INPUT_SLOT_Y = 17;
    public static final int OUTPUT_SLOT_X = 116;
    public static final int OUTPUT_SLOT_Y = 35;

    /** Inner fill of the status pit on {@code textures/gui/machine/solar_furnace.png}. */
    public static final int SOLAR_INDICATOR_X = 60;
    public static final int SOLAR_INDICATOR_Y = 59;
    public static final int SOLAR_INDICATOR_SIZE = 8;

    /** Empty thunder outline; packed lit bolt is the same 16×16 crop at u=177,v=1. */
    public static final int THUNDER_X = 56;
    public static final int THUNDER_Y = 38;
    public static final int THUNDER_W = 16;
    public static final int THUNDER_H = 16;

    private final SolarFurnaceBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final RecipePropertySet acceptedInputs;
    private final int machineSlotStart;
    private final int playerInvStart;

    public SolarFurnaceMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public SolarFurnaceMenu(int containerId, Inventory playerInv, SolarFurnaceBlockEntity be) {
        super(ModMenus.SOLAR_FURNACE.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = new SimpleContainerData(DATA_SIZE);
        this.acceptedInputs = playerInv.player.level().recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT);

        List<MachineUpgradeSlot> upgrades = new ArrayList<>();
        UpgradeInventory handler = be.upgrades();
        int upgradeSlotYBias = TAB_Y_BIAS - PanelLayout.TAB_SIZE;
        for (int i = 0; i < UpgradeConfig.UPGRADE_SLOT_COUNT; i++) {
            MachineUpgradeSlot slot = new MachineUpgradeSlot(handler, i, upgradeSlotYBias);
            this.addSlot(slot);
            upgrades.add(slot);
        }
        this.upgradeSlots = Collections.unmodifiableList(upgrades);
        this.machineSlotStart = UpgradeConfig.UPGRADE_SLOT_COUNT;

        this.addSlot(new ResourceHandlerSlot(
                be.items(),
                be.items()::set,
                SolarFurnaceBlockEntity.SLOT_INPUT,
                INPUT_SLOT_X,
                INPUT_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return canSmelt(stack);
            }
        });
        this.addSlot(new ResourceHandlerSlot(
                be.items(),
                be.items()::set,
                SolarFurnaceBlockEntity.SLOT_OUTPUT,
                OUTPUT_SLOT_X,
                OUTPUT_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                be.awardExperience(player);
                super.onTake(player, stack);
            }
        });
        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, 84);
        syncData();
        this.addDataSlots(data);
    }

    private static SolarFurnaceBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof SolarFurnaceBlockEntity furnace) {
            return furnace;
        }
        throw new IllegalStateException("Missing solar furnace at " + pos);
    }

    private void syncData() {
        data.set(DATA_COOK_PROGRESS, be.cookingProgressSynced());
        data.set(DATA_COOK_TOTAL, be.cookingTotalSynced());
        data.set(DATA_REDSTONE, be.redstoneMode().ordinal());
        SolarPower.Snapshot solar = be.solarSnapshot();
        data.set(DATA_SOLAR_PERMILLE, solar.permille());
        data.set(DATA_SOLAR_STATUS, solar.status().ordinal());
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public SolarFurnaceBlockEntity blockEntity() {
        return be;
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return index >= 0 && index < machineSlotStart;
    }

    public float progressFraction() {
        int max = data.get(DATA_COOK_TOTAL);
        if (max <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) data.get(DATA_COOK_PROGRESS) / (float) max);
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public void setRedstoneMode(RedstoneMode mode) {
        be.setRedstoneMode(mode);
        syncData();
        broadcastChanges();
    }

    public int solarPermille() {
        return data.get(DATA_SOLAR_PERMILLE);
    }

    /** 0–1 sun height relative to noon (ignores cook-speed peak). */
    public float solarStrengthFraction() {
        if (solarStatus() != SolarPower.Status.WORKING) {
            return 0.0F;
        }
        return Mth.clamp(solarPermille() / 1000.0F, 0.0F, 1.0F);
    }

    public SolarPower.Status solarStatus() {
        int ordinal = data.get(DATA_SOLAR_STATUS);
        SolarPower.Status[] values = SolarPower.Status.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SolarPower.Status.NO_SUN;
        }
        return values[ordinal];
    }

    public boolean canSmelt(ItemStack stack) {
        return !stack.isEmpty() && acceptedInputs.test(stack);
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

        int inputSlot = machineSlotStart;
        int outputSlot = machineSlotStart + 1;

        if (index < machineSlotStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == outputSlot) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, remaining);
        } else if (index == inputSlot) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (canSmelt(stack)) {
                if (!this.moveItemStackTo(stack, inputSlot, inputSlot + 1, false)) {
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
        return remaining;
    }
}
