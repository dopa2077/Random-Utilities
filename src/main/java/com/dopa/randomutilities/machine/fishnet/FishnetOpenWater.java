package com.dopa.randomutilities.machine.fishnet;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Mirrors {@link FishingHook}'s private open-water checks so fishnet treasure loot
 * matches vanilla (5×5 water columns, air above — not a small pond).
 */
final class FishnetOpenWater {
    private FishnetOpenWater() {}

    static void applyToBobber(FishingHook bobber, boolean openWater) {
        bobber.openWater = openWater;
    }

    static void applyToBobber(FishingHook bobber, Level level, BlockPos bobberPos) {
        applyToBobber(bobber, calculateOpenWater(level, bobberPos));
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
        OpenWaterType type = null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = from.getX(); x <= to.getX(); x++) {
            for (int y = from.getY(); y <= to.getY(); y++) {
                for (int z = from.getZ(); z <= to.getZ(); z++) {
                    OpenWaterType next = getOpenWaterTypeForBlock(level, cursor.set(x, y, z));
                    if (type == null) {
                        type = next;
                    } else if (type != next) {
                        return OpenWaterType.INVALID;
                    }
                }
            }
        }
        return type == null ? OpenWaterType.INVALID : type;
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
