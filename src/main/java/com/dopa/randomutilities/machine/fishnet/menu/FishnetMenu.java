package com.dopa.randomutilities.machine.fishnet.menu;

import com.dopa.randomutilities.machine.fishnet.FishnetBlockEntity;
import com.dopa.randomutilities.core.machine.RedstoneMode;
import com.dopa.randomutilities.core.machine.UpgradeInventory;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.core.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.core.gui.panel.PanelLayout;

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

public class FishnetMenu extends AbstractContainerMenu {
    public static final int TAB_Y_BIAS = 0;

    public static final int DATA_PROGRESS = FishnetBlockEntity.DATA_PROGRESS;
    public static final int DATA_TOTAL = FishnetBlockEntity.DATA_TOTAL;
    public static final int DATA_REDSTONE = FishnetBlockEntity.DATA_REDSTONE;
    public static final int DATA_UNDERWATER = FishnetBlockEntity.DATA_UNDERWATER;
    public static final int DATA_HAS_ROD = FishnetBlockEntity.DATA_HAS_ROD;
    public static final int DATA_PARTICLES = FishnetBlockEntity.DATA_PARTICLES;
    public static final int DATA_SOUND = FishnetBlockEntity.DATA_SOUND;
    public static final int DATA_SIZE = FishnetBlockEntity.DATA_COUNT;

    public static final int ROD_X = 26;
    public static final int ROD_Y = 35;
    /** Centered in the gap between the rod slot and the 3×3 catch grid. */
    public static final int ARROW_X = 58;
    public static final int ARROW_Y = 35;
    public static final int GRID_LEFT = 98;
    public static final int GRID_TOP = 17;

    private final FishnetBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int machineSlotStart;
    private final int rodSlotIndex;
    private final int playerInvStart;

    public FishnetMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf.readBlockPos()));
    }

    public FishnetMenu(int containerId, Inventory playerInv, FishnetBlockEntity be) {
        super(ModMenus.FISHNET.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = new SimpleContainerData(DATA_SIZE);

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
        this.rodSlotIndex = this.slots.size();

        this.addSlot(new ResourceHandlerSlot(be.rod(), be.rod()::set, 0, ROD_X, ROD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FishnetBlockEntity.isFishingRod(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new ResourceHandlerSlot(
                        be.items(),
                        be.items()::set,
                        index,
                        GRID_LEFT + col * 18,
                        GRID_TOP + row * 18
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        this.playerInvStart = this.slots.size();
        this.addStandardInventorySlots(playerInv, 8, 84);
        syncData();
        this.addDataSlots(data);
    }

    private static FishnetBlockEntity resolveBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof FishnetBlockEntity fishnet) {
            return fishnet;
        }
        throw new IllegalStateException("Missing fishnet at " + pos);
    }

    private void syncData() {
        data.set(DATA_PROGRESS, be.catchProgress());
        data.set(DATA_TOTAL, be.catchTotal());
        data.set(DATA_REDSTONE, be.redstoneMode().ordinal());
        data.set(DATA_UNDERWATER, be.isUnderwater() ? 1 : 0);
        data.set(DATA_HAS_ROD, be.hasRod() ? 1 : 0);
        data.set(DATA_PARTICLES, be.particlesEnabled() ? 1 : 0);
        data.set(DATA_SOUND, be.soundEnabled() ? 1 : 0);
    }

    @Override
    public void broadcastChanges() {
        syncData();
        super.broadcastChanges();
    }

    public FishnetBlockEntity blockEntity() {
        return be;
    }

    public List<MachineUpgradeSlot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotIndex(int index) {
        return index >= 0 && index < machineSlotStart;
    }

    public boolean isRodSlotIndex(int index) {
        return index == rodSlotIndex;
    }

    public float progressFraction() {
        int max = data.get(DATA_TOTAL);
        if (max <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) data.get(DATA_PROGRESS) / (float) max);
    }

    public boolean isUnderwater() {
        return data.get(DATA_UNDERWATER) != 0;
    }

    public boolean hasRod() {
        return data.get(DATA_HAS_ROD) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    public void setRedstoneMode(RedstoneMode mode) {
        be.setRedstoneMode(mode);
        syncData();
        broadcastChanges();
    }

    public boolean isParticlesEnabled() {
        return data.get(DATA_PARTICLES) != 0;
    }

    public void setParticlesEnabled(boolean enabled) {
        be.setParticlesEnabled(enabled);
        syncData();
        broadcastChanges();
    }

    public boolean isSoundEnabled() {
        return data.get(DATA_SOUND) != 0;
    }

    public void setSoundEnabled(boolean enabled) {
        be.setSoundEnabled(enabled);
        syncData();
        broadcastChanges();
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

        if (index < machineSlotStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == rodSlotIndex) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < playerInvStart) {
            if (!this.moveItemStackTo(stack, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (be.upgrades().accepts(stack)) {
                if (!this.moveItemStackTo(stack, 0, machineSlotStart, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (FishnetBlockEntity.isFishingRod(stack)) {
                if (!this.moveItemStackTo(stack, rodSlotIndex, rodSlotIndex + 1, false)) {
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
