package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class dOPasJeiPlugin implements IModPlugin {
    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ResourceGeneratorRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<GeneratorJeiRecipe> recipes = new ArrayList<>();
        for (GeneratorType type : GeneratorType.values()) {
            for (GeneratorRecipe recipe : GeneratorRecipeConfig.getRecipes(type)) {
                recipes.add(new GeneratorJeiRecipe(type, recipe));
            }
        }
        registration.addRecipes(ResourceGeneratorRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemLike[] generators = {
                ModBlocks.BASIC_STONE_GENERATOR,
                ModBlocks.INTERMEDIATE_STONE_GENERATOR,
                ModBlocks.ADVANCED_STONE_GENERATOR,
                ModBlocks.ELITE_STONE_GENERATOR,
                ModBlocks.ULTIMATE_STONE_GENERATOR,
                ModBlocks.CREATIVE_STONE_GENERATOR,
                ModBlocks.RANDOM_ORE_GENERATOR,
                ModBlocks.METAL_BLOCK_GENERATOR,
                ModBlocks.CREATIVE_RANDOM_ORE_GENERATOR,
                ModBlocks.CREATIVE_METAL_BLOCK_GENERATOR
        };
        registration.addCraftingStation(ResourceGeneratorRecipeCategory.RECIPE_TYPE, generators);
    }
}
