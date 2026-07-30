package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

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
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (GeneratorType type : GeneratorType.values()) {
            registration.addRecipeCategories(new ResourceGeneratorRecipeCategory(guiHelper, type));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (GeneratorType type : GeneratorType.values()) {
            List<GeneratorJeiRecipe> recipes = new ArrayList<>();
            for (GeneratorRecipe recipe : GeneratorRecipeConfig.getRecipes(type)) {
                recipes.add(new GeneratorJeiRecipe(type, recipe));
            }
            registration.addRecipes(ResourceGeneratorRecipeCategory.recipeType(type), recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (GeneratorType type : GeneratorType.values()) {
            ItemStack stack = generatorStack(type);
            if (stack.isEmpty()) {
                continue;
            }
            registration.addCraftingStation(ResourceGeneratorRecipeCategory.recipeType(type), stack);
        }
    }

    private static ItemStack generatorStack(GeneratorType type) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, type.id())
        );
        if (block.asItem() == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(block.asItem());
    }
}
