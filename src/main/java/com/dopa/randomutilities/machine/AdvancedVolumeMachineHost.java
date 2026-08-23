package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.util.WorkingVolume;

/** Shared surface for advanced breaker/placer block entities and their menus. */
public interface AdvancedVolumeMachineHost {
    WorkingVolume workingVolume();

    MachineEnergy energy();

    EnergyMachineUpgradeInventory upgrades();

    boolean whitelistMode();

    void setWhitelistMode(boolean whitelistMode);

    default boolean isMuted() {
        return false;
    }

    default void setMuted(boolean mute) {
    }

    RedstoneMode redstoneMode();

    void setRedstoneMode(RedstoneMode mode);

    int overlayColor();

    void setOverlayColor(int overlayColor);

    int maxRange();

    void setRangeX(int value);

    void setRangeY(int value);

    void setRangeZ(int value);

    void setOffsetX(int value);

    void setOffsetY(int value);

    void setOffsetZ(int value);
}
