package com.dopa.randomutilities.fishnet;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.lang.reflect.Field;

/**
 * Mirrors {@link FishingHook}'s private open-water checks so fishnet treasure loot
 * matches vanilla (5×5 water columns, air above — not a small pond).
 */
final class FishnetOpenWater {
    private static final Field OPEN_WATER;

    static {
        try {
            OPEN_WATER = FishingHook.class.getDeclaredField("openWater");
            OPEN_WATER.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private FishnetOpenWater() {}

    static void applyToBobber(FishingHook bobber, boolean openWater) {
        setOpenWater(bobber, openWater);
    }

    static void applyToBobber(FishingHook bobber, Level level, BlockPos bobberPos) {
        applyToBobber(bobber, calculateOpenWater(level, bobberPos));
    }

    private static void setOpenWater(FishingHook bobber, boolean openWater) {
        try {
            OPEN_WATER.setBoolean(bobber, openWater);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set FishingHook.openWater", e);
        }
    }

    /** Same rules as {@code FishingHook#calculateOpenWater}. */
    static boolean calculateOpenWater(Level level, BlockPos blockPos) {
        OpenWaterType previousLayer = OpenWaterType.INVALID;

        for (int y = -1; y <= 2; y++) {
            OpenWaterType layer = getOpenWaterTypeForArea(
                    level, blockPos.offset(-2, y, -2), blockPos.offset(2, y, 2));
            switch (layer) {
                case ABOVE_WATER -> {
                    if (previousLayer == OpenWaterType.INVALID) {
                        return false;
                    }
                }
                case INSIDE_WATER -> {
                    if (previousLayer == OpenWaterType.ABOVE_WATER) {
                        return false;
                    }
                }
                case INVALID -> {
                    return false;
                }
            }
            previousLayer = layer;
        }
        return true;
    }

    private static OpenWaterType getOpenWaterTypeForArea(Level level, BlockPos from, BlockPos to) {
        return BlockPos.betweenClosedStream(from, to)
                .map(pos -> getOpenWaterTypeForBlock(level, pos))
                .reduce((a, b) -> a == b ? a : OpenWaterType.INVALID)
                .orElse(OpenWaterType.INVALID);
    }

    private static OpenWaterType getOpenWaterTypeForBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Waterlogged fishnet is the bobber site; treat it as open water like a water source.
        if (state.getBlock() instanceof FishnetBlock && state.getValue(FishnetBlock.WATERLOGGED)) {
            return OpenWaterType.INSIDE_WATER;
        }
        if (!state.isAir() && !state.is(Blocks.LILY_PAD)) {
            FluidState fluidState = state.getFluidState();
            return fluidState.is(FluidTags.WATER)
                    && fluidState.isSource()
                    && state.getCollisionShape(level, pos).isEmpty()
                    ? OpenWaterType.INSIDE_WATER
                    : OpenWaterType.INVALID;
        }
        return OpenWaterType.ABOVE_WATER;
    }

    private enum OpenWaterType {
        ABOVE_WATER,
        INSIDE_WATER,
        INVALID
    }
}
