package com.dopa.randomutilities.itemcollector.client;

import com.dopa.randomutilities.client.WorkingVolumeOverlay;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Collector range overlay tracker — delegates to {@link WorkingVolumeOverlay}
 * so volume machines and collectors share one client set.
 */
public final class ItemCollectorClientOverlay {
    private ItemCollectorClientOverlay() {}

    public static boolean isEnabled(ResourceKey<Level> dimension, BlockPos pos) {
        return WorkingVolumeOverlay.isEnabled(dimension, pos);
    }

    public static void setEnabled(ResourceKey<Level> dimension, BlockPos pos, boolean enabled) {
        WorkingVolumeOverlay.setEnabled(dimension, pos, enabled);
    }

    public static void toggle(ResourceKey<Level> dimension, BlockPos pos) {
        WorkingVolumeOverlay.toggle(dimension, pos);
    }

    public static void clear(ResourceKey<Level> dimension, BlockPos pos) {
        WorkingVolumeOverlay.setEnabled(dimension, pos, false);
    }

    public static Set<BlockPos> enabledPositions(ResourceKey<Level> dimension) {
        return WorkingVolumeOverlay.enabledPositions(dimension);
    }

    public static void dropIfEmpty(ResourceKey<Level> dimension) {
        WorkingVolumeOverlay.dropIfEmpty(dimension);
    }

    public static void pruneRemoved(Level level, ResourceKey<Level> dimension) {
        WorkingVolumeOverlay.pruneRemoved(level, dimension);
    }
}
