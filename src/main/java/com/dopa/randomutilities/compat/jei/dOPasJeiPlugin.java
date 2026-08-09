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
import com.dopa.randomutilities.machine.solarfurnace.client.SolarFurnaceScreen;
import com.dopa.randomutilities.fishnet.client.FishnetScreen;
import com.dopa.randomutilities.fishnet.config.TreasureLootConfig;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.trashcan.client.TrashCanScreen;
import com.dopa.randomutilities.redstoneclock.client.RedstoneClockScreen;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collection;
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

            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(
                    ResourceGeneratorScreen screen, double guiMouseX, double guiMouseY) {
                return List.of(IGuiClickableArea.createBasic(
                        ResourceGeneratorScreen.ARROW_X,
                        ResourceGeneratorScreen.ARROW_Y,
                        ResourceGeneratorScreen.ARROW_W,
                        ResourceGeneratorScreen.ARROW_H,
                        ResourceGeneratorRecipeCategory.recipeType(screen.getMenu().generatorType())
                ));
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
        registration.addGuiContainerHandler(SolarFurnaceScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(SolarFurnaceScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(FishnetScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(FishnetScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addRecipeClickArea(
                SolarFurnaceScreen.class,
                79, 34, 24, 16,
                RecipeTypes.SMELTING
        );
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
        registerIngredientInfos(registration);
    }

    private static void registerIngredientInfos(IRecipeRegistration registration) {
        info(registration, ModItems.SOLAR_FURNACE.get(), "jei.dopasrandomutilities.solar_furnace.info");
        info(registration, ModItems.FISHNET.get(), "jei.dopasrandomutilities.fishnet.info");
        info(registration, ModItems.DEV_NULL.get(), "jei.dopasrandomutilities.dev_null.info");
        info(registration, ModItems.ADVANCED_DEV_NULL.get(), "jei.dopasrandomutilities.advanced_dev_null.info");
        info(registration, ModItems.MINI_CHEST.get(), "jei.dopasrandomutilities.mini_chest.info");
        info(registration, ModItems.TRASH_CAN.get(), "jei.dopasrandomutilities.trash_can.info");
        info(registration, ModItems.REDSTONE_CLOCK.get(), "jei.dopasrandomutilities.redstone_clock.info");
        info(registration, ModItems.BASIC_ITEM_COLLECTOR.get(), "jei.dopasrandomutilities.basic_item_collector.info");
        info(registration, ModItems.ADVANCED_ITEM_COLLECTOR.get(), "jei.dopasrandomutilities.advanced_item_collector.info");
        info(registration, ModItems.UPGRADE_CASING.get(), "jei.dopasrandomutilities.upgrade_casing.info");
        info(registration, ModItems.PRODUCTIVITY_UPGRADE.get(), "jei.dopasrandomutilities.productivity_upgrade.info");
        info(registration, ModItems.OVERCLOCK_UPGRADE.get(), "jei.dopasrandomutilities.overclock_upgrade.info");
        info(registration, ModItems.FORTUNE_MESH_UPGRADE.get(), "jei.dopasrandomutilities.fortune_mesh_upgrade.info");
        registerTreasureMeshInfo(registration);

        for (GeneratorType type : GeneratorType.values()) {
            String modeKey = switch (type.mode()) {
                case RECIPE -> "jei.dopasrandomutilities.generator.stone.info";
                case RANDOM_ORE -> "jei.dopasrandomutilities.generator.random_ore.info";
                case METAL_BLOCK -> "jei.dopasrandomutilities.generator.metal_block.info";
            };
            boolean creative = type == GeneratorType.CREATIVE_STONE
                    || type == GeneratorType.CREATIVE_RANDOM_ORE
                    || type == GeneratorType.CREATIVE_METAL_BLOCK;
            if (creative) {
                registration.addIngredientInfo(
                        ModItems.forType(type).get(),
                        Component.translatable(modeKey),
                        Component.translatable("jei.dopasrandomutilities.generator.creative_note")
                );
            } else {
                info(registration, ModItems.forType(type).get(), modeKey);
            }
        }
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable(key));
    }
    private static void registerTreasureMeshInfo(IRecipeRegistration registration) {
        // JEI registers once at startup — reload from disk here so we don't use jar defaults
        // that were loaded before config/dopas_random_utilities/items/treasure_loot.json.
        TreasureLootConfig.load();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.dopasrandomutilities.treasure_mesh_upgrade.info"));
        // JEI drops empty components; a space keeps a visible blank line.
        lines.add(Component.literal(" "));
        lines.add(Component.translatable("jei.dopasrandomutilities.treasure_mesh_upgrade.weight_never"));
        lines.add(Component.translatable("jei.dopasrandomutilities.treasure_mesh_upgrade.weight_always"));
        lines.add(Component.literal(" "));
        List<TreasureLootConfig.Entry> entries = new ArrayList<>(TreasureLootConfig.entries());
        entries.sort((a, b) -> Double.compare(b.weight(), a.weight()));
        if (entries.isEmpty()) {
            lines.add(Component.translatable("jei.dopasrandomutilities.treasure_mesh_upgrade.loot_empty"));
        } else {
            for (TreasureLootConfig.Entry entry : entries) {
                lines.add(Component.translatable(
                        "jei.dopasrandomutilities.treasure_mesh_upgrade.loot_entry",
                        entry.displayName(),
                        formatWeight(entry.weight())
                ));
            }
        }
        registration.addIngredientInfo(
                ModItems.TREASURE_MESH_UPGRADE.get(),
                lines.toArray(new Component[0])
        );
    }

    private static String formatWeight(double weight) {
        if (weight == Math.rint(weight)) {
            return Integer.toString((int) weight);
        }
        String text = String.format(java.util.Locale.ROOT, "%.4f", weight);
        int end = text.length();
        while (end > 1 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 1 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }


    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (GeneratorType type : GeneratorType.values()) {
            ItemStack stack = GeneratorJeiRecipe.stackFor(type);
            if (!stack.isEmpty()) {
                registration.addCraftingStation(ResourceGeneratorRecipeCategory.recipeType(type), stack);
            }
        }
        registration.addCraftingStation(RecipeTypes.SMELTING, ModItems.SOLAR_FURNACE.get());
    }
}
