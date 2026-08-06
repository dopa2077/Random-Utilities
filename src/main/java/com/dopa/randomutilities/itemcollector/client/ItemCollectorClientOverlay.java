package com.dopa.randomutilities.itemcollector.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only tracker for which collectors should draw their range overlay. */
public final class ItemCollectorClientOverlay {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ENABLED = new ConcurrentHashMap<>();

    private ItemCollectorClientOverlay() {}

    public static boolean isEnabled(ResourceKey<Level> dimension, BlockPos pos) {
        Set<BlockPos> set = ENABLED.get(dimension);
        return set != null && set.contains(pos.immutable());
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

    public static void clear(ResourceKey<Level> dimension, BlockPos pos) {
        setEnabled(dimension, pos, false);
    }

    public static Set<BlockPos> enabledPositions(ResourceKey<Level> dimension) {
        Set<BlockPos> set = ENABLED.get(dimension);
        return set == null ? Set.of() : Set.copyOf(set);
    }

    public static void removeMissing(ResourceKey<Level> dimension, Set<BlockPos> stillPresent) {
        Set<BlockPos> set = ENABLED.get(dimension);
        if (set == null) {
            return;
        }
        Iterator<BlockPos> it = set.iterator();
        while (it.hasNext()) {
            if (!stillPresent.contains(it.next())) {
                it.remove();
            }
        }
        if (set.isEmpty()) {
            ENABLED.remove(dimension);
        }
    }
}
