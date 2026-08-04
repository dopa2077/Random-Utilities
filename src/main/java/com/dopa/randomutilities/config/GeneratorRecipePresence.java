package com.dopa.randomutilities.config;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Shapeless presence checks for generator recipe ghost UI.
 * Side resources are ordered by recipe definition; not bound to world sides.
 */
public final class GeneratorRecipePresence {
    public static final int SIDE_SLOT_COUNT = GeneratorRecipe.SIDE_COUNT;
    public static final int BELOW_SLOT = SIDE_SLOT_COUNT;
    public static final int GHOST_SLOT_COUNT = SIDE_SLOT_COUNT + 1;

    private GeneratorRecipePresence() {}

    /** Compact list of non-null side resources in recipe order. */
    public static List<GeneratorResource> orderedSideResources(GeneratorRecipe recipe) {
        List<GeneratorResource> list = new ArrayList<>(SIDE_SLOT_COUNT);
        for (GeneratorResource resource : recipe.resources()) {
            if (resource != null) {
                list.add(resource);
            }
        }
        return list;
    }

    /**
     * Bit flags: bits 0–3 = ordered side inputs missing, bit 4 = below missing.
     * Only set for slots that the recipe actually requires.
     */
    public static int missingFlags(Level level, BlockPos generatorPos, GeneratorRecipe recipe) {
        List<GeneratorRecipe.Neighbor> neighbors = GeneratorRecipe.scanHorizontalNeighbors(level, generatorPos);
        BitSet used = new BitSet(neighbors.size());
        int flags = 0;
        List<GeneratorResource> ordered = orderedSideResources(recipe);
        for (int slot = 0; slot < ordered.size() && slot < SIDE_SLOT_COUNT; slot++) {
            GeneratorResource required = ordered.get(slot);
            int found = findUnusedNeighbor(neighbors, used, required);
            if (found < 0) {
                flags |= 1 << slot;
            } else {
                used.set(found);
            }
        }
        Block under = level.getBlockState(generatorPos.below()).getBlock();
        if (recipe.requiredUnder() != null && recipe.requiredUnder() != under) {
            flags |= 1 << BELOW_SLOT;
        }
        return flags;
    }

    public static ItemStack ghostStack(@Nullable GeneratorResource resource) {
        if (resource == null) {
            return ItemStack.EMPTY;
        }
        if (resource.isBlock()) {
            return new ItemStack(resource.block().asItem());
        }
        if (resource.isFluid()) {
            return fluidBucket(resource.fluid());
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack belowGhostStack(GeneratorRecipe recipe) {
        return recipe.requiredUnder() == null
                ? ItemStack.EMPTY
                : new ItemStack(recipe.requiredUnder().asItem());
    }

    private static ItemStack fluidBucket(@Nullable Fluid fluid) {
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return ItemStack.EMPTY;
        }
        if (fluid.isSame(Fluids.WATER)) {
            return new ItemStack(Items.WATER_BUCKET);
        }
        if (fluid.isSame(Fluids.LAVA)) {
            return new ItemStack(Items.LAVA_BUCKET);
        }
        return new ItemStack(Items.BUCKET);
    }

    private static int findUnusedNeighbor(
            List<GeneratorRecipe.Neighbor> neighbors,
            BitSet used,
            GeneratorResource required
    ) {
        for (int i = 0; i < neighbors.size(); i++) {
            if (used.get(i)) {
                continue;
            }
            GeneratorRecipe.Neighbor neighbor = neighbors.get(i);
            if (required.isFluid()) {
                if (neighbor.fluid() != null && required.fluid().isSame(neighbor.fluid())) {
                    return i;
                }
            } else if (required.isBlock() && required.block() == neighbor.block()) {
                return i;
            }
        }
        return -1;
    }
}
