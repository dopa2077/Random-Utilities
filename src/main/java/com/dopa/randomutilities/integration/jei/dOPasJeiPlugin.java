package com.dopa.randomutilities.integration.jei;

import com.dopa.randomutilities.machine.breaker.client.AdvancedBlockBreakerScreen;
import com.dopa.randomutilities.machine.breaker.menu.AdvancedBlockBreakerMenu;
import com.dopa.randomutilities.machine.placer.client.AdvancedBlockPlacerScreen;
import com.dopa.randomutilities.machine.placer.client.SimpleBlockPlacerScreen;
import com.dopa.randomutilities.machine.placer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.integration.jei.generator.GeneratorJeiRecipe;
import com.dopa.randomutilities.core.filter.network.GhostFilterPayload;
import com.dopa.randomutilities.machine.fishnet.client.FishnetScreen;
import com.dopa.randomutilities.logistics.collector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.integration.jei.generator.ResourceGeneratorRecipeCategory;
import com.dopa.randomutilities.config.FeatureConfig;
import com.dopa.randomutilities.config.ModContentIds;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.core.filter.client.FilterScreen;
import com.dopa.randomutilities.logistics.collector.client.ItemCollectorScreen;
import com.dopa.randomutilities.item.magnet.client.MagnetScreen;
import com.dopa.randomutilities.item.magnet.menu.MagnetMenu;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.generator.client.ResourceGeneratorScreen;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipe;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.machine.combustion.client.CombustionGeneratorScreen;
import com.dopa.randomutilities.machine.solar.panel.client.SolarPanelControllerScreen;
import com.dopa.randomutilities.machine.solar.furnace.client.SolarFurnaceScreen;
import com.dopa.randomutilities.block.trashcan.TrashCanMenu;
import com.dopa.randomutilities.logistics.transfer.HeadKind;
import com.dopa.randomutilities.logistics.transfer.TransferNodeItem;
import com.dopa.randomutilities.logistics.transfer.client.TransferEnergyScreen;
import com.dopa.randomutilities.logistics.transfer.client.TransferFilterScreen;
import com.dopa.randomutilities.logistics.transfer.client.TransferNodeScreen;
import com.dopa.randomutilities.logistics.transfer.menu.TransferFilterMenu;
import com.dopa.randomutilities.logistics.transfer.menu.TransferNodeMenu;
import com.dopa.randomutilities.machine.fishnet.config.TreasureLootConfig;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModItems;
import net.neoforged.neoforge.registries.DeferredItem;
import com.dopa.randomutilities.registry.ModTags;
import com.dopa.randomutilities.block.trashcan.client.TrashCanScreen;
import com.dopa.randomutilities.block.redstoneclock.client.RedstoneClockScreen;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
        registration.addGuiContainerHandler(MagnetScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(MagnetScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(TransferNodeScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(TransferNodeScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(TransferEnergyScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(TransferEnergyScreen screen) {
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
        registration.addGuiContainerHandler(SimpleBlockPlacerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(SimpleBlockPlacerScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(AdvancedBlockPlacerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AdvancedBlockPlacerScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(AdvancedBlockBreakerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AdvancedBlockBreakerScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(CombustionGeneratorScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(CombustionGeneratorScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
        registration.addGuiContainerHandler(SolarPanelControllerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(SolarPanelControllerScreen screen) {
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
        registration.addGhostIngredientHandler(ItemCollectorScreen.class, new FilterGhostJeiHandler<>(
                gui -> gui.getMenu().collectorType().filterSlotCount(),
                gui -> gui.leftPos() + ItemCollectorMenu.filterSlotX(gui.getMenu().collectorType()),
                gui -> gui.topPos() + ItemCollectorMenu.FILTER_SLOT_Y,
                (slot, stack) -> ClientPacketDistributor.sendToServer(new GhostFilterPayload(slot, stack))
        ));
        registration.addGhostIngredientHandler(MagnetScreen.class, new FilterGhostJeiHandler<>(
                gui -> MagnetMenu.FILTER_SLOT_COUNT,
                gui -> gui.leftPos() + MagnetMenu.FILTER_SLOT_X,
                gui -> gui.topPos() + MagnetMenu.FILTER_SLOT_Y,
                (slot, stack) -> ClientPacketDistributor.sendToServer(new GhostFilterPayload(slot, stack))
        ));
        registration.addGhostIngredientHandler(TrashCanScreen.class, new FilterGhostJeiHandler<>(
                gui -> TrashCanMenu.FILTER_SLOT_COUNT,
                gui -> gui.leftPos() + TrashCanMenu.FILTER_SLOT_X,
                gui -> gui.topPos() + TrashCanMenu.FILTER_SLOT_Y,
                (slot, stack) -> ClientPacketDistributor.sendToServer(new GhostFilterPayload(slot, stack))
        ));
        registration.addGhostIngredientHandler(TransferNodeScreen.class, new FilterGhostJeiHandler<>(
                gui -> TransferNodeMenu.FILTER_SLOT_COUNT,
                gui -> gui.leftPos() + TransferNodeMenu.FILTER_SLOT_X,
                gui -> gui.topPos() + TransferNodeMenu.FILTER_SLOT_Y,
                (slot, stack) -> ClientPacketDistributor.sendToServer(new GhostFilterPayload(slot, stack))
        ));
        registration.addGhostIngredientHandler(AdvancedBlockPlacerScreen.class, new FilterGhostJeiHandler<>(
                gui -> AdvancedBlockPlacerMenu.FILTER_SLOT_COUNT,
                gui -> gui.leftPos() + AdvancedBlockPlacerMenu.FILTER_SLOT_X,
                gui -> gui.topPos() + AdvancedBlockPlacerMenu.FILTER_SLOT_Y,
                (slot, stack) -> ClientPacketDistributor.sendToServer(new GhostFilterPayload(slot, stack))
        ));
        registration.addGhostIngredientHandler(AdvancedBlockBreakerScreen.class, new FilterGhostJeiHandler<>(
                gui -> AdvancedBlockBreakerMenu.FILTER_SLOT_COUNT,
                gui -> gui.leftPos() + AdvancedBlockBreakerMenu.FILTER_SLOT_X,
                gui -> gui.topPos() + AdvancedBlockBreakerMenu.FILTER_SLOT_Y,
                (slot, stack) -> ClientPacketDistributor.sendToServer(new GhostFilterPayload(slot, stack))
        ));
        registration.addGhostIngredientHandler(TransferFilterScreen.class, new FilterGhostJeiHandler<>(
                gui -> TransferFilterMenu.SLOT_COUNT,
                gui -> gui.leftPos() + TransferFilterMenu.GRID_X,
                gui -> gui.topPos() + TransferFilterMenu.GRID_Y,
                (slot, stack) -> ClientPacketDistributor.sendToServer(new GhostFilterPayload(slot, stack)),
                TransferFilterMenu.GRID,
                TransferFilterMenu.SLOT
        ));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (GeneratorType type : GeneratorType.values()) {
            if (ModBlocks.forType(type) != null) {
                registration.addRecipeCategories(new ResourceGeneratorRecipeCategory(guiHelper, type));
            }
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (GeneratorType type : GeneratorType.values()) {
            if (ModBlocks.forType(type) == null) {
                continue;
            }
            List<GeneratorJeiRecipe> recipes = new ArrayList<>();
            for (GeneratorRecipe recipe : GeneratorRecipeConfig.getRecipes(type)) {
                recipes.add(new GeneratorJeiRecipe(type, recipe));
            }
            registration.addRecipes(ResourceGeneratorRecipeCategory.recipeType(type), recipes);
        }
        registerIngredientInfos(registration);
    }

    private static void registerIngredientInfos(IRecipeRegistration registration) {
        infoIfPresent(registration, ModItems.SOLAR_FURNACE, "jei.dopasrandomutilities.solar_furnace.info");
        infoIfPresent(registration, ModItems.COMBUSTION_GENERATOR, "jei.dopasrandomutilities.combustion_generator.info");
        infoIfPresent(registration, ModItems.SOLAR_PANEL_CONTROLLER, "jei.dopasrandomutilities.solar_panel_controller.info");
        infoIfPresent(registration, ModItems.SOLAR_PANEL_TIER1, "jei.dopasrandomutilities.solar_panel.info");
        infoIfPresent(registration, ModItems.SOLAR_PANEL_TIER2, "jei.dopasrandomutilities.solar_panel.info");
        infoIfPresent(registration, ModItems.SOLAR_PANEL_TIER3, "jei.dopasrandomutilities.solar_panel.info");
        infoIfPresent(registration, ModItems.FISHNET, "jei.dopasrandomutilities.fishnet.info");
        infoIfPresent(registration, ModItems.SIMPLE_BLOCK_BREAKER, "jei.dopasrandomutilities.simple_block_breaker.info");
        infoIfPresent(registration, ModItems.ADVANCED_BLOCK_BREAKER, "jei.dopasrandomutilities.advanced_block_breaker.info");
        infoIfPresent(registration, ModItems.SIMPLE_BLOCK_PLACER, "jei.dopasrandomutilities.simple_block_placer.info");
        infoIfPresent(registration, ModItems.ADVANCED_BLOCK_PLACER, "jei.dopasrandomutilities.advanced_block_placer.info");
        infoIfPresent(registration, ModItems.DEV_NULL, "jei.dopasrandomutilities.dev_null.info");
        infoIfPresent(registration, ModItems.ADVANCED_DEV_NULL, "jei.dopasrandomutilities.advanced_dev_null.info");
        infoIfPresent(registration, ModItems.MINI_CHEST, "jei.dopasrandomutilities.mini_chest.info");
        infoIfPresent(registration, ModItems.TRASH_CAN, "jei.dopasrandomutilities.trash_can.info");
        infoIfPresent(registration, ModItems.REDSTONE_CLOCK, "jei.dopasrandomutilities.redstone_clock.info");
        infoIfPresent(registration, ModItems.BASIC_ITEM_COLLECTOR, "jei.dopasrandomutilities.basic_item_collector.info");
        infoIfPresent(registration, ModItems.ADVANCED_ITEM_COLLECTOR, "jei.dopasrandomutilities.advanced_item_collector.info");
        infoIfPresent(registration, ModItems.ITEM_MAGNET, "jei.dopasrandomutilities.item_magnet.info");
        infoIfPresent(registration, ModItems.SIMPLE_CORE_FRAME, "jei.dopasrandomutilities.simple_core_frame.info");
        infoIfPresent(registration, ModItems.ADVANCED_CORE_FRAME, "jei.dopasrandomutilities.advanced_core_frame.info");
        infoIfPresent(registration, ModItems.LASSO, "jei.dopasrandomutilities.lasso.info");
        infoIfPresent(registration, ModItems.GOLDEN_LASSO, "jei.dopasrandomutilities.golden_lasso.info");
        infoIfPresent(registration, ModItems.CURSED_LASSO, "jei.dopasrandomutilities.cursed_lasso.info");
        infoIfPresent(registration, ModItems.TINY_TNT, "jei.dopasrandomutilities.tiny_tnt.info");
        infoIfPresent(registration, ModItems.WOOD_CHIP, "jei.dopasrandomutilities.wood_chip.info");
        List<ItemStack> transferPipes = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.TRANSFER_PIPES).forEach(holder ->
                transferPipes.add(new ItemStack(holder.value())));
        if (!transferPipes.isEmpty()) {
            registration.addItemStackInfo(
                    transferPipes,
                    Component.translatable("jei.dopasrandomutilities.transfer_pipe.info")
            );
        }
        infoIfPresent(registration, ModItems.TRANSFER_NODE, "jei.dopasrandomutilities.transfer_node.info");
        if (FeatureConfig.isItemEnabled(ModContentIds.TRANSFER_NODE_FLUID)) {
            registration.addItemStackInfo(
                    TransferNodeItem.create(HeadKind.FLUID),
                    Component.translatable("jei.dopasrandomutilities.transfer_node_fluid.info")
            );
        }
        if (FeatureConfig.isItemEnabled(ModContentIds.TRANSFER_NODE_ENERGY)) {
            registration.addItemStackInfo(
                    TransferNodeItem.create(HeadKind.ENERGY),
                    Component.translatable("jei.dopasrandomutilities.transfer_node_energy.info")
            );
        }
        infoIfPresent(registration, ModItems.FILTER, "jei.dopasrandomutilities.filter.info");
        infoIfPresent(registration, ModItems.UPGRADE_CASING, "jei.dopasrandomutilities.upgrade_casing.info");
        infoIfPresent(registration, ModItems.PRODUCTIVITY_UPGRADE, "jei.dopasrandomutilities.productivity_upgrade.info");
        infoIfPresent(registration, ModItems.OVERCLOCK_UPGRADE, "jei.dopasrandomutilities.overclock_upgrade.info");
        infoIfPresent(registration, ModItems.STACK_UPGRADE, "jei.dopasrandomutilities.stack_upgrade.info");
        infoIfPresent(registration, ModItems.ENERGY_UPGRADE, "jei.dopasrandomutilities.energy_upgrade.info");
        infoIfPresent(registration, ModItems.FLUID_CAPACITY_UPGRADE, "jei.dopasrandomutilities.fluid_capacity_upgrade.info");
        infoIfPresent(registration, ModItems.EFFICIENCY_UPGRADE, "jei.dopasrandomutilities.efficiency_upgrade.info");
        if (ModItems.RANGE_UPGRADE != null) {
            registration.addIngredientInfo(
                    ModItems.RANGE_UPGRADE.get(),
                    Component.translatable(
                            "jei.dopasrandomutilities.range_upgrade.info",
                            Integer.toString(UpgradeConfig.rangeBonus())
                    )
            );
        }
        infoIfPresent(registration, ModItems.FORTUNE_MESH_UPGRADE, "jei.dopasrandomutilities.fortune_mesh_upgrade.info");
        registerTreasureMeshInfo(registration);

        for (GeneratorType type : GeneratorType.values()) {
            DeferredItem<?> generatorItem = ModItems.forType(type);
            if (generatorItem == null) {
                continue;
            }
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
                        generatorItem.get(),
                        Component.translatable(modeKey),
                        Component.translatable("jei.dopasrandomutilities.generator.creative_note")
                );
            } else {
                info(registration, generatorItem.get(), modeKey);
            }
        }
    }

    private static void infoIfPresent(IRecipeRegistration registration, DeferredItem<?> item, String key) {
        if (item != null) {
            info(registration, item.get(), key);
        }
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable(key));
    }

    private static void registerTreasureMeshInfo(IRecipeRegistration registration) {
        if (ModItems.TREASURE_MESH_UPGRADE == null) {
            return;
        }
        // JEI registers once at startup — reload from disk here so we don't use jar defaults only.
        TreasureLootConfig.load();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.dopasrandomutilities.treasure_mesh_upgrade.info"));
        // JEI drops empty components; a space keeps a visible blank line.
        lines.add(Component.literal(" "));
        lines.add(Component.translatable("jei.dopasrandomutilities.treasure_mesh_upgrade.weight_relative"));
        lines.add(Component.literal(" "));
        List<TreasureLootConfig.Entry> entries = new ArrayList<>(TreasureLootConfig.entries());
        entries.sort((a, b) -> Double.compare(b.weight(), a.weight()));
        double totalWeight = 0.0;
        for (TreasureLootConfig.Entry entry : entries) {
            if (entry.weight() > 0.0) {
                totalWeight += entry.weight();
            }
        }
        if (entries.isEmpty()) {
            lines.add(Component.translatable("jei.dopasrandomutilities.treasure_mesh_upgrade.loot_empty"));
        } else {
            for (TreasureLootConfig.Entry entry : entries) {
                lines.add(Component.translatable(
                        "jei.dopasrandomutilities.treasure_mesh_upgrade.loot_entry",
                        entry.displayName(),
                        formatWeightAndChance(entry.weight(), totalWeight)
                ));
            }
        }
        registration.addIngredientInfo(
                ModItems.TREASURE_MESH_UPGRADE.get(),
                lines.toArray(new Component[0])
        );
    }

    private static String formatWeightAndChance(double weight, double total) {
        String weightText = formatWeight(weight);
        if (weight <= 0.0 || total <= 0.0) {
            return weightText;
        }
        return weightText + " (" + formatWeight(100.0 * weight / total) + "%)";
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
        if (ModItems.SOLAR_FURNACE != null) {
            registration.addCraftingStation(RecipeTypes.SMELTING, ModItems.SOLAR_FURNACE.get());
        }
    }
}
