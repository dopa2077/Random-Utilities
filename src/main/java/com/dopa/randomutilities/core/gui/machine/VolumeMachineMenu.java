package com.dopa.randomutilities.core.gui.machine;

import com.dopa.randomutilities.core.machine.RedstoneMode;
import com.dopa.randomutilities.core.machine.UpgradeInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Shared menu surface for advanced breaker/placer volume machines. */
public interface VolumeMachineMenu {
    int getRangeX();

    int getRangeY();

    int getRangeZ();

    int getOffsetX();

    int getOffsetY();

    int getOffsetZ();

    boolean isWhitelistMode();

    boolean isMuted();

    int getOverlayColor();

    int maxRange();

    int energyStored();

    int energyCapacity();

    int energyUsage();

    int energyMaxReceive();

    RedstoneMode redstoneMode();

    UpgradeInventory upgrades();

    java.util.List<com.dopa.randomutilities.core.machine.menu.MachineUpgradeSlot> getUpgradeSlots();

    int filterSlotStart();

    int filterSlotCount();

    int iconX();

    int filterSlotY();

    int energyBarX();

    int energyBarY();

    int energyBarW();

    int energyBarH();

    boolean isUpgradeSlotIndex(int index);

    boolean isPickaxeSlotIndex(int index);

    void applySetting(byte kind, int value);

    BlockPos machinePos();

    @Nullable
    Level machineLevel();
}
