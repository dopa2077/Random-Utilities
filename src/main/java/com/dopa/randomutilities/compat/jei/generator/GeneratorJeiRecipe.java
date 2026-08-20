package com.dopa.randomutilities.compat.jei.generator;

import com.dopa.randomutilities.generator.config.GeneratorOutputMode;
import com.dopa.randomutilities.generator.config.GeneratorRecipe;
import com.dopa.randomutilities.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.generator.config.GeneratorResource;
import com.dopa.randomutilities.generator.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.List;

public record GeneratorJeiRecipe(GeneratorType type, GeneratorRecipe recipe) {
    public static ItemStack stackFor(GeneratorType type) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, type.id())
        );
        return block.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(block.asItem());
    }
    public Identifier recipeId() {
        return Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, type.id() + "/" + recipe.id());
    }

    public ItemStack generatorStack() {
        return stackFor(type);
    }

    public List<SideIngredient> sideIngredients() {
        List<SideIngredient> sides = new ArrayList<>(4);
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            GeneratorResource resource = recipe.resources().get(i);
            if (resource != null) {
                sides.add(new SideIngredient(resource, recipe.consume()[i]));
            }
        }
        return sides;
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

    public int resultFluidMillibuckets() {
        return recipe.isFluidResult() ? recipe.amount() : 0;
    }

    public String amountLabel() {
        if (recipe.isFluidResult()) {
            double buckets = recipe.amount() / (double) FluidType.BUCKET_VOLUME;
            if (Math.abs(buckets - Math.rint(buckets)) < 1.0E-6D) {
                return Integer.toString((int) Math.rint(buckets));
            }
            String formatted = String.format(java.util.Locale.ROOT, "%.4f", buckets)
                    .replaceAll("0+$", "")
                    .replaceAll("\\.$", "");
            return formatted.isEmpty() ? "0" : formatted;
        }
        return Integer.toString(recipe.amount());
    }

    public List<ItemStack> resultStacks() {
        if (recipe.isFluidResult()) {
            return List.of();
        }
        if (!recipe.isRandomResult()) {
            Item output = recipe.outputItem();
            return output == null || output == Items.AIR
                    ? List.of()
                    : List.of(new ItemStack(output, recipe.amount()));
        }
        List<Block> pool = switch (type.mode()) {
            case RANDOM_ORE -> GeneratorRecipeConfig.ores();
            case METAL_BLOCK -> GeneratorRecipeConfig.metalBlocks();
            case RECIPE -> List.of();
        };
        List<ItemStack> stacks = new ArrayList<>(pool.size());
        for (Block block : pool) {
            if (block.asItem() != Items.AIR) {
                stacks.add(new ItemStack(block.asItem(), recipe.amount()));
            }
        }
        return stacks;
    }

    public record SideIngredient(GeneratorResource resource, boolean consume) {}
}