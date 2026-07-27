package com.dopa.randomutilities.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

public final class GeneratorRecipeMatcher {
    public record Neighbor(BlockPos pos, @Nullable Fluid fluid, Block block) {}

    public record Match(GeneratorRecipe recipe, BlockPos[] resourcePositions) {
        public Match {
            resourcePositions = Arrays.copyOf(resourcePositions, GeneratorRecipe.SIDE_COUNT);
        }
    }

    private GeneratorRecipeMatcher() {}

    public static List<Neighbor> scanHorizontalNeighbors(Level level, BlockPos center) {
        List<Neighbor> neighbors = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = center.relative(direction);
            BlockState state = level.getBlockState(neighborPos);
            FluidState fluidState = level.getFluidState(neighborPos);
            Fluid fluid = countsAsFluidSource(fluidState) ? fluidState.getType() : null;
            neighbors.add(new Neighbor(neighborPos, fluid, state.getBlock()));
        }
        return neighbors;
    }

    public static boolean countsAsFluidSource(FluidState fluidState) {
        if (fluidState.isEmpty()) {
            return false;
        }
        return fluidState.isSource() || fluidState.getAmount() >= 8;
    }

    /**
     * Picks the most specific recipe whose requirements are fully satisfied.
     * Side resources can match any of the four horizontal neighbors (order-independent).
     */
    public static Optional<Match> findBestMatch(Level level, BlockPos generatorPos, List<GeneratorRecipe> recipes) {
        if (recipes.isEmpty()) {
            return Optional.empty();
        }

        List<Neighbor> neighbors = scanHorizontalNeighbors(level, generatorPos);
        Block underBlock = level.getBlockState(generatorPos.below()).getBlock();

        for (GeneratorRecipe recipe : recipes) {
            Optional<Match> match = tryMatch(recipe, neighbors, underBlock);
            if (match.isPresent()) {
                return match;
            }
        }

        return Optional.empty();
    }

    private static Optional<Match> tryMatch(
            GeneratorRecipe recipe,
            List<Neighbor> neighbors,
            Block underBlock
    ) {
        if (!recipe.matchesUnderBlock(underBlock)) {
            return Optional.empty();
        }

        BlockPos[] matched = new BlockPos[GeneratorRecipe.SIDE_COUNT];
        BitSet usedNeighbors = new BitSet(neighbors.size());

        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            GeneratorResource required = recipe.resources().get(i);
            if (required == null) {
                continue;
            }

            Optional<Integer> neighborIndex = findUnusedNeighbor(neighbors, usedNeighbors, required);
            if (neighborIndex.isEmpty()) {
                return Optional.empty();
            }

            int index = neighborIndex.get();
            usedNeighbors.set(index);
            matched[i] = neighbors.get(index).pos();
        }

        return Optional.of(new Match(recipe, matched));
    }

    private static Optional<Integer> findUnusedNeighbor(
            List<Neighbor> neighbors,
            BitSet usedNeighbors,
            GeneratorResource required
    ) {
        for (int i = 0; i < neighbors.size(); i++) {
            if (usedNeighbors.get(i)) {
                continue;
            }
            if (matches(neighbors.get(i), required)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private static boolean matches(Neighbor neighbor, GeneratorResource required) {
        if (required.isFluid()) {
            return neighbor.fluid() != null && required.fluid().isSame(neighbor.fluid());
        }
        if (required.isBlock()) {
            return required.block() == neighbor.block();
        }
        return false;
    }
}
