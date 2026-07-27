package com.dopa.randomutilities.config;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public record GeneratorRecipe(
        String id,
        Block result,
        Fluid fluid1,
        Fluid fluid2,
        boolean consume1,
        boolean consume2,
        Block requiredUnder,
        int ticks,
        int priority
) {
    public boolean matchesFluids(Fluid first, Fluid second) {
        return (fluid1.isSame(first) && fluid2.isSame(second))
                || (fluid1.isSame(second) && fluid2.isSame(first));
    }

    public boolean matchesUnderBlock(Block block) {
        return requiredUnder == null || requiredUnder == block;
    }

    public record FluidAssignment(BlockPos fluid1Pos, BlockPos fluid2Pos) {}
}
