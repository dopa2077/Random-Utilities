package com.dopa.randomutilities.config;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record GeneratorRecipe(
        String id,
        Block result,
        List<@Nullable GeneratorResource> resources,
        boolean[] consume,
        @Nullable Block requiredUnder,
        int ticks,
        int amount
) {
    public static final int SIDE_COUNT = 4;

    public GeneratorRecipe {
        if (resources.size() != SIDE_COUNT) {
            throw new IllegalArgumentException("Expected " + SIDE_COUNT + " resources");
        }
        if (consume.length != SIDE_COUNT) {
            throw new IllegalArgumentException("Expected " + SIDE_COUNT + " consume flags");
        }
        // Allow null slots (unused sides); List.copyOf does not.
        resources = Collections.unmodifiableList(new ArrayList<>(resources));
        consume = consume.clone();
        amount = Math.max(1, amount);
        ticks = Math.max(1, ticks);
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

    /** How many placement requirements this recipe has; more specific recipes win ties. */
    public int specificity() {
        int score = resourceCount();
        if (requiredUnder != null) {
            score++;
        }
        return score;
    }
}
