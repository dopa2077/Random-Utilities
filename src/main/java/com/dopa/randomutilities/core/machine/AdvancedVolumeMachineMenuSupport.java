package com.dopa.randomutilities.core.machine;

import com.dopa.randomutilities.core.util.WorkingVolume;

import net.minecraft.world.inventory.ContainerData;

/** Shared {@link ContainerData} indices and sync for advanced volume-machine menus. */
public final class AdvancedVolumeMachineMenuSupport {
    public static final int DATA_RANGE_X = 0;
    public static final int DATA_RANGE_Y = 1;
    public static final int DATA_RANGE_Z = 2;
    public static final int DATA_OFFSET_X = 3;
    public static final int DATA_OFFSET_Y = 4;
    public static final int DATA_OFFSET_Z = 5;
    public static final int DATA_WHITELIST = 6;
    public static final int DATA_MUTE = 7;
    public static final int DATA_REDSTONE = 8;
    public static final int DATA_ENERGY_STORED = 9;
    public static final int DATA_ENERGY_CAPACITY = 10;
    public static final int DATA_ENERGY_USAGE = 11;
    public static final int DATA_ENERGY_MAX_RECEIVE = 12;
    public static final int DATA_MAX_RANGE = 13;
    public static final int DATA_OVERLAY_COLOR = 14;
    public static final int DATA_SIZE = 15;

    private AdvancedVolumeMachineMenuSupport() {}

    public static void syncData(ContainerData data, AdvancedVolumeMachineHost host) {
        WorkingVolume volume = host.workingVolume();
        data.set(DATA_RANGE_X, volume.rangeX());
        data.set(DATA_RANGE_Y, volume.rangeY());
        data.set(DATA_RANGE_Z, volume.rangeZ());
        data.set(DATA_OFFSET_X, volume.offsetX());
        data.set(DATA_OFFSET_Y, volume.offsetY());
        data.set(DATA_OFFSET_Z, volume.offsetZ());
        data.set(DATA_WHITELIST, host.whitelistMode() ? 1 : 0);
        data.set(DATA_MUTE, host.isMuted() ? 1 : 0);
        data.set(DATA_REDSTONE, host.redstoneMode().ordinal());
        data.set(DATA_ENERGY_STORED, host.energy().stored());
        data.set(DATA_ENERGY_CAPACITY, host.energy().capacity());
        data.set(DATA_ENERGY_USAGE, host.energy().lastTickUsage());
        data.set(DATA_ENERGY_MAX_RECEIVE, host.energy().maxReceive());
        data.set(DATA_MAX_RANGE, host.maxRange());
        data.set(DATA_OVERLAY_COLOR, host.overlayColor());
    }

    public static void applySetting(ContainerData data, AdvancedVolumeMachineHost host, byte kind, int value) {
        switch (kind) {
            case WorkingVolume.KIND_RANGE_X -> {
                host.setRangeX(value);
                data.set(DATA_RANGE_X, host.workingVolume().rangeX());
            }
            case WorkingVolume.KIND_RANGE_Y -> {
                host.setRangeY(value);
                data.set(DATA_RANGE_Y, host.workingVolume().rangeY());
            }
            case WorkingVolume.KIND_RANGE_Z -> {
                host.setRangeZ(value);
                data.set(DATA_RANGE_Z, host.workingVolume().rangeZ());
            }
            case WorkingVolume.KIND_OFFSET_X -> {
                host.setOffsetX(value);
                data.set(DATA_OFFSET_X, host.workingVolume().offsetX());
            }
            case WorkingVolume.KIND_OFFSET_Y -> {
                host.setOffsetY(value);
                data.set(DATA_OFFSET_Y, host.workingVolume().offsetY());
            }
            case WorkingVolume.KIND_OFFSET_Z -> {
                host.setOffsetZ(value);
                data.set(DATA_OFFSET_Z, host.workingVolume().offsetZ());
            }
            case WorkingVolume.KIND_MUTE -> {
                host.setMuted(value != 0);
                data.set(DATA_MUTE, host.isMuted() ? 1 : 0);
            }
            case WorkingVolume.KIND_FILTER_MODE -> {
                host.setWhitelistMode(value != 0);
                data.set(DATA_WHITELIST, host.whitelistMode() ? 1 : 0);
            }
            case WorkingVolume.KIND_REDSTONE -> {
                host.setRedstoneMode(RedstoneMode.byOrdinal(value));
                data.set(DATA_REDSTONE, host.redstoneMode().ordinal());
            }
            case WorkingVolume.KIND_COLOR -> {
                host.setOverlayColor(value);
                data.set(DATA_OVERLAY_COLOR, host.overlayColor());
            }
            default -> {
            }
        }
    }
}
