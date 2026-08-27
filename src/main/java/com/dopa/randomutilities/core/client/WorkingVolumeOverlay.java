package com.dopa.randomutilities.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only tracker for which working-volume machines should draw their range overlay. */
public final class WorkingVolumeOverlay {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ENABLED = new ConcurrentHashMap<>();

    private WorkingVolumeOverlay() {}

    public static boolean isEnabled(ResourceKey<Level> dimension, BlockPos pos) {
        Set<BlockPos> set = ENABLED.get(dimension);
        BlockPos key = pos.immutable();
        if (set == null || !set.contains(key)) {
            return false;
        }
        pruneIfRemoved(Minecraft.getInstance().level, dimension, key);
        set = ENABLED.get(dimension);
        return set != null && set.contains(key);
    }

    public static void setEnabled(ResourceKey<Level> dimension, BlockPos pos, boolean enabled) {
        BlockPos key = pos.immutable();
        if (enabled) {
            ENABLED.computeIfAbsent(dimension, d -> ConcurrentHashMap.newKeySet()).add(key);
        } else {
            Set<BlockPos> set = ENABLED.get(dimension);
            if (set != null) {
                set.remove(key);
                if (set.isEmpty()) {
                    ENABLED.remove(dimension);
                }
            }
        }
    }

    public static void toggle(ResourceKey<Level> dimension, BlockPos pos) {
        setEnabled(dimension, pos, !isEnabled(dimension, pos));
    }

    public static Set<BlockPos> enabledPositions(ResourceKey<Level> dimension) {
        Set<BlockPos> set = ENABLED.get(dimension);
        return set == null ? Set.of() : set;
    }

    public static void dropIfEmpty(ResourceKey<Level> dimension) {
        Set<BlockPos> set = ENABLED.get(dimension);
        if (set != null && set.isEmpty()) {
            ENABLED.remove(dimension);
        }
    }

    /** Drops overlay entries whose block entities were removed. Unloaded chunks are kept. */
    public static void pruneRemoved(Level level, ResourceKey<Level> dimension) {
        Set<BlockPos> set = ENABLED.get(dimension);
        if (set == null || set.isEmpty()) {
            return;
        }
        Iterator<BlockPos> it = set.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) == null) {
                it.remove();
            }
        }
        dropIfEmpty(dimension);
    }

    private static void pruneIfRemoved(@Nullable Level level, ResourceKey<Level> dimension, BlockPos pos) {
        if (level == null || !level.dimension().equals(dimension) || !level.isLoaded(pos)) {
            return;
        }
        if (level.getBlockEntity(pos) != null) {
            return;
        }
        Set<BlockPos> set = ENABLED.get(dimension);
        if (set != null) {
            set.remove(pos);
            dropIfEmpty(dimension);
        }
    }
}
