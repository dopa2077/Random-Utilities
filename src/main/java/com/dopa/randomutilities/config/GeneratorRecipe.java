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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record GeneratorRecipe(
        String id,
        @Nullable Block result,
        @Nullable Fluid resultFluid,
        List<@Nullable GeneratorResource> resources,
        boolean[] consume,
        @Nullable Block requiredUnder,
        int ticks,
        int amount,
        GeneratorOutputMode outputMode
) {
    public static final int SIDE_COUNT = 4;

    public GeneratorRecipe {
        if (resources.size() != SIDE_COUNT) {
            throw new IllegalArgumentException("Expected " + SIDE_COUNT + " resources");
        }
        if (consume.length != SIDE_COUNT) {
            throw new IllegalArgumentException("Expected " + SIDE_COUNT + " consume flags");
        }
        if (result != null && resultFluid != null) {
            throw new IllegalArgumentException("Recipe cannot have both a block and fluid result");
        }
        resources = Collections.unmodifiableList(new ArrayList<>(resources));
        consume = consume.clone();
        amount = Math.max(1, amount);
        ticks = Math.max(1, ticks);
        if (outputMode == null) {
            outputMode = GeneratorOutputMode.INSERT;
        }
    }

    public boolean isRandomResult() {
        return result == null && resultFluid == null;
    }

    public boolean isFluidResult() {
        return resultFluid != null;
    }

    public boolean matchesUnderBlock(Block block) {
        return requiredUnder == null || requiredUnder == block;
    }

    public int resourceCount() {
        int count = 0;
        for (GeneratorResource resource : resources) {
            if (resource != null) {
                count++;
            }
        }
        return count;
    }

    public int specificity() {
        int score = resourceCount();
        if (requiredUnder != null) {
            score++;
        }
        return score;
    }

    public record Neighbor(BlockPos pos, @Nullable Fluid fluid, Block block) {}

    public record Match(GeneratorRecipe recipe, BlockPos[] resourcePositions) {
        public Match {
            resourcePositions = Arrays.copyOf(resourcePositions, SIDE_COUNT);
        }
    }

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
        return !fluidState.isEmpty() && (fluidState.isSource() || fluidState.getAmount() >= 8);
    }

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

    private static Optional<Match> tryMatch(GeneratorRecipe recipe, List<Neighbor> neighbors, Block underBlock) {
        if (!recipe.matchesUnderBlock(underBlock)) {
            return Optional.empty();
        }
        BlockPos[] matched = new BlockPos[SIDE_COUNT];
        BitSet usedNeighbors = new BitSet(neighbors.size());
        for (int i = 0; i < SIDE_COUNT; i++) {
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
