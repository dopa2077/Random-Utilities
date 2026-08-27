package com.dopa.randomutilities.machine.generator.config;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * A side requirement that matches either a fluid source or a solid block.
 */
public record GeneratorResource(@Nullable Fluid fluid, @Nullable Block block) {
    public static GeneratorResource ofFluid(Fluid fluid) {
        return new GeneratorResource(fluid, null);
    }

    public static GeneratorResource ofBlock(Block block) {
        return new GeneratorResource(null, block);
    }

    public boolean isFluid() {
        return fluid != null;
    }

    public boolean isBlock() {
        return block != null;
    }
}
