package com.dopa.randomutilities.core.gui.machine;

import com.dopa.randomutilities.core.machine.MachineEnergy;
import com.dopa.randomutilities.core.machine.UpgradeInventory;
import com.dopa.randomutilities.core.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Two-click confirm before taking energy upgrades that would shrink the FE buffer. */
public final class EnergyUpgradeRemoveConfirm {
    private int pendingSlot = -1;

    public boolean pendingFor(@Nullable Slot slot, UpgradeInventory upgrades, int stored) {
        if (slot == null || slot.index != pendingSlot) {
            return false;
        }
        int removing = energyCount(slot);
        if (removing <= 0 || !MachineEnergy.wouldVoidEnergy(stored, upgrades.energyCount() - removing)) {
            pendingSlot = -1;
            return false;
        }
        return true;
    }

    /**
     * @return {@code true} if this click should be swallowed (first confirm click)
     */
    public boolean block(
            @Nullable Slot slot,
            int button,
            ContainerInput type,
            ItemStack carried,
            UpgradeInventory upgrades,
            int stored
    ) {
        if (!(slot instanceof MachineUpgradeSlot)) {
            pendingSlot = -1;
            return false;
        }
        int removing = energyUpgradesRemoved(slot, button, type, carried);
        if (removing <= 0 || !MachineEnergy.wouldVoidEnergy(stored, upgrades.energyCount() - removing)) {
            if (pendingSlot == slot.index) {
                pendingSlot = -1;
            }
            return false;
        }
        if (pendingSlot == slot.index) {
            pendingSlot = -1;
            return false;
        }
        pendingSlot = slot.index;
        return true;
    }

    private static int energyCount(Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !stack.is(ModItems.ENERGY_UPGRADE.get())) {
            return 0;
        }
        return stack.getCount();
    }

    private static int energyUpgradesRemoved(Slot slot, int button, ContainerInput type, ItemStack carried) {
        int count = energyCount(slot);
        if (count <= 0) {
            return 0;
        }
        return switch (type) {
            case QUICK_MOVE, SWAP, PICKUP_ALL -> count;
            case THROW -> button == 1 ? count : 1;
            case PICKUP -> pickupRemoved(count, button, carried);
            default -> 0;
        };
    }

    private static int pickupRemoved(int count, int button, ItemStack carried) {
        if (carried.isEmpty()) {
            return button == 1 ? Math.max(1, (count + 1) / 2) : count;
        }
        if (carried.is(ModItems.ENERGY_UPGRADE.get())) {
            return 0;
        }
        return count;
    }
}
