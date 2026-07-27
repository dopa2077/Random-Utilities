package com.dopa.randomutilities.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GeneratorRecipeMatcher {
    public record FluidNeighbor(BlockPos pos, Fluid fluid) {}

    public record Match(GeneratorRecipe recipe, BlockPos fluid1Pos, BlockPos fluid2Pos) {}

    private GeneratorRecipeMatcher() {}

    public static List<FluidNeighbor> scanHorizontalFluids(Level level, BlockPos center) {
        List<FluidNeighbor> neighbors = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = center.relative(direction);
            FluidState fluidState = level.getFluidState(neighborPos);
            if (countsAsFluidSource(fluidState)) {
                neighbors.add(new FluidNeighbor(neighborPos, fluidState.getType()));
            }
        }
        return neighbors;
    }

    public static boolean countsAsFluidSource(FluidState fluidState) {
        if (fluidState.isEmpty()) {
            return false;
        }
        return fluidState.isSource() || fluidState.getAmount() >= 8;
    }

    public static Optional<Match> findBestMatch(Level level, BlockPos generatorPos, List<GeneratorRecipe> recipes) {
        if (recipes.isEmpty()) {
            return Optional.empty();
        }

        List<FluidNeighbor> fluidNeighbors = scanHorizontalFluids(level, generatorPos);
        if (fluidNeighbors.size() < 2) {
            return Optional.empty();
        }

        BlockState underState = level.getBlockState(generatorPos.below());
        Block underBlock = underState.getBlock();

        for (GeneratorRecipe recipe : recipes) {
            Optional<GeneratorRecipe.FluidAssignment> assignment = findFluidAssignment(recipe, fluidNeighbors);
            if (assignment.isEmpty()) {
                continue;
            }

            if (recipe.matchesUnderBlock(underBlock)) {
                GeneratorRecipe.FluidAssignment fluids = assignment.get();
                return Optional.of(new Match(recipe, fluids.fluid1Pos(), fluids.fluid2Pos()));
            }
        }

        GeneratorRecipe defaultRecipe = GeneratorRecipeConfig.getDefaultRecipe();
        if (defaultRecipe != null) {
            return findFluidAssignment(defaultRecipe, fluidNeighbors)
                    .map(fluids -> new Match(defaultRecipe, fluids.fluid1Pos(), fluids.fluid2Pos()));
        }

        return Optional.empty();
    }

    private static Optional<GeneratorRecipe.FluidAssignment> findFluidAssignment(
            GeneratorRecipe recipe,
            List<FluidNeighbor> fluidNeighbors
    ) {
        for (int firstIndex = 0; firstIndex < fluidNeighbors.size(); firstIndex++) {
            for (int secondIndex = 0; secondIndex < fluidNeighbors.size(); secondIndex++) {
                if (firstIndex == secondIndex) {
                    continue;
                }

                FluidNeighbor first = fluidNeighbors.get(firstIndex);
                FluidNeighbor second = fluidNeighbors.get(secondIndex);

                if (recipe.fluid1().isSame(first.fluid()) && recipe.fluid2().isSame(second.fluid())) {
                    return Optional.of(new GeneratorRecipe.FluidAssignment(first.pos(), second.pos()));
                }
                if (recipe.fluid1().isSame(second.fluid()) && recipe.fluid2().isSame(first.fluid())) {
                    return Optional.of(new GeneratorRecipe.FluidAssignment(second.pos(), first.pos()));
                }
            }
        }
        return Optional.empty();
    }
}
