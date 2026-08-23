package com.dopa.randomutilities.gui.machine;

import com.dopa.randomutilities.util.WorkingVolume;

import net.minecraft.client.gui.Font;

/** Shared GUI host for advanced breaker/placer volume, mute, and redstone panels. */
public interface VolumeMachineGui extends MachineRedstonePanel.Host {
    Font getFont();

    void clearFocus();

    boolean isShiftHeld();

    int rangeX();

    int rangeY();

    int rangeZ();

    int offsetX();

    int offsetY();

    int offsetZ();

    boolean isMuted();

    int overlayColor();

    /** Current max range including range upgrades. */
    default int maxRange() {
        return WorkingVolume.MAX_RANGE;
    }

    /** Hard cap for volume offset steppers (independent of range upgrades). */
    default int maxOffset() {
        return WorkingVolume.MAX_OFFSET;
    }

    void sendVolumeSetting(byte kind, int value);

    default int volumeValue(byte kind) {
        return switch (kind) {
            case WorkingVolume.KIND_RANGE_X -> rangeX();
            case WorkingVolume.KIND_RANGE_Y -> rangeY();
            case WorkingVolume.KIND_RANGE_Z -> rangeZ();
            case WorkingVolume.KIND_OFFSET_X -> offsetX();
            case WorkingVolume.KIND_OFFSET_Y -> offsetY();
            case WorkingVolume.KIND_OFFSET_Z -> offsetZ();
            default -> 0;
        };
    }
}
