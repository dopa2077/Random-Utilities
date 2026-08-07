package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.compat.jei.generator.GeneratorJeiRecipe;
import com.dopa.randomutilities.compat.jei.generator.ResourceGeneratorRecipeCategory;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.client.FilterScreen;
import com.dopa.randomutilities.itemcollector.client.ItemCollectorScreen;
import com.dopa.randomutilities.machine.generator.client.ResourceGeneratorScreen;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipe;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.trashcan.client.TrashCanScreen;
import com.dopa.randomutilities.redstoneclock.client.RedstoneClockScreen;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

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
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                VanillaTypes.ITEM_STACK,
                List.of(
                        new ItemStack(ModItems.UI_TEST_ITEM.get()),
                        new ItemStack(ModItems.UI_TEST_BLOCK_ITEM.get())
                )
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(FilterScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(FilterScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(ResourceGeneratorScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(ResourceGeneratorScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(ItemCollectorScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(ItemCollectorScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(RedstoneClockScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(RedstoneClockScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGhostIngredientHandler(ItemCollectorScreen.class, new ItemCollectorJeiHandler());
        registration.addGhostIngredientHandler(TrashCanScreen.class, new TrashCanJeiHandler());
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
            ItemStack stack = GeneratorJeiRecipe.stackFor(type);
            if (!stack.isEmpty()) {
                registration.addCraftingStation(ResourceGeneratorRecipeCategory.recipeType(type), stack);
            }
        }
    }
}
