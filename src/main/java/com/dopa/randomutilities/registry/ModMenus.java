package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.menu.FilterMenu;
import com.dopa.randomutilities.machine.generator.menu.ResourceGeneratorMenu;
import com.dopa.randomutilities.minichest.MiniChestMenu;
import com.dopa.randomutilities.itemcollector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.trashcan.TrashCanMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, dOPasRandomUtilities.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> FILTER =
            MENUS.register("filter", () -> IMenuTypeExtension.create(FilterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ResourceGeneratorMenu>> RESOURCE_GENERATOR =
            MENUS.register("resource_generator", () -> IMenuTypeExtension.create(ResourceGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MiniChestMenu>> MINI_CHEST =
            MENUS.register("mini_chest", () -> IMenuTypeExtension.create(MiniChestMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TrashCanMenu>> TRASH_CAN =
            MENUS.register("trash_can", () -> IMenuTypeExtension.create(TrashCanMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ItemCollectorMenu>> ITEM_COLLECTOR =
            MENUS.register("item_collector", () -> IMenuTypeExtension.create(ItemCollectorMenu::new));

    private ModMenus() {}
}
