package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.config.GeneratedBlockLists;
import com.dopa.randomutilities.config.GeneratorOutputMode;
import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorResource;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI display wrapper around a config-driven {@link GeneratorRecipe}.
 */
public record GeneratorJeiRecipe(GeneratorType type, GeneratorRecipe recipe) {
    public Identifier recipeId() {
        return Identifier.fromNamespaceAndPath(
                dOPasRandomUtilities.MOD_ID,
                type.id() + "/" + recipe.id()
        );
    }

    public Block generatorBlock() {
        return BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, type.id())
        );
    }

    public ItemStack generatorStack() {
        Block block = generatorBlock();
        if (block.asItem() == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(block.asItem());
    }

    public List<SideIngredient> sideIngredients() {
        List<SideIngredient> sides = new ArrayList<>(4);
        List<GeneratorResource> resources = recipe.resources();
        boolean[] consume = recipe.consume();
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            GeneratorResource resource = resources.get(i);
            if (resource == null) {
                continue;
            }
            sides.add(new SideIngredient(resource, consume[i]));
        }
        return sides;
    }

    public @Nullable Block belowBlock() {
        return recipe.requiredUnder();
    }

    public boolean isInsertOutput() {
        return recipe.outputMode() == GeneratorOutputMode.INSERT || recipe.isFluidResult();
    }

    public boolean isDropOutput() {
        return !isInsertOutput() && recipe.outputMode() == GeneratorOutputMode.DROP;
    }

    public boolean isPlaceOutput() {
        return !isInsertOutput() && recipe.outputMode() == GeneratorOutputMode.PLACE;
    }

    public boolean isFluidResult() {
        return recipe.isFluidResult();
    }

    public @Nullable Fluid resultFluid() {
        return recipe.resultFluid();
    }

    /** Millibuckets shown in JEI for fluid results ({@code amount} buckets). */
    public int resultFluidMillibuckets() {
        long millibuckets = (long) recipe.amount() * FluidType.BUCKET_VOLUME;
        if (millibuckets > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) millibuckets;
    }

    public List<ItemStack> resultStacks() {
        if (recipe.isFluidResult()) {
            return List.of();
        }
        if (!recipe.isRandomResult()) {
            Block result = recipe.result();
            if (result == null || result.asItem() == Items.AIR) {
                return List.of();
            }
            return List.of(new ItemStack(result.asItem(), recipe.amount()));
        }

        List<Block> pool = switch (type.mode()) {
            case RANDOM_ORE -> GeneratedBlockLists.ores();
            case METAL_BLOCK -> GeneratedBlockLists.metalBlocks();
            case RECIPE -> List.of();
        };
        if (pool.isEmpty()) {
            return List.of();
        }

        List<ItemStack> stacks = new ArrayList<>(pool.size());
        for (Block block : pool) {
            if (block.asItem() != Items.AIR) {
                stacks.add(new ItemStack(block.asItem(), recipe.amount()));
            }
        }
        return stacks;
    }

    public record SideIngredient(GeneratorResource resource, boolean consume) {
        public boolean isFluid() {
            return resource.isFluid();
        }

        public @Nullable Fluid fluid() {
            return resource.fluid();
        }

        public @Nullable Block block() {
            return resource.block();
        }
    }
}
