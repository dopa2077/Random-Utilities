package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.blockbreaker.menu.AdvancedBlockBreakerMenu;
import com.dopa.randomutilities.blockplacer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.blockplacer.menu.SimpleBlockPlacerMenu;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.menu.FilterMenu;
import com.dopa.randomutilities.generator.menu.ResourceGeneratorMenu;
import com.dopa.randomutilities.solarfurnace.menu.SolarFurnaceMenu;
import com.dopa.randomutilities.fishnet.menu.FishnetMenu;
import com.dopa.randomutilities.minichest.MiniChestMenu;
import com.dopa.randomutilities.itemcollector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.magnet.menu.MagnetMenu;
import com.dopa.randomutilities.trashcan.TrashCanMenu;
import com.dopa.randomutilities.redstoneclock.RedstoneClockMenu;
import com.dopa.randomutilities.transfer.menu.TransferEnergyMenu;
import com.dopa.randomutilities.transfer.menu.TransferFilterMenu;
import com.dopa.randomutilities.transfer.menu.TransferNodeMenu;

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

    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneClockMenu>> REDSTONE_CLOCK =
            MENUS.register("redstone_clock", () -> IMenuTypeExtension.create(RedstoneClockMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ItemCollectorMenu>> ITEM_COLLECTOR =
            MENUS.register("item_collector", () -> IMenuTypeExtension.create(ItemCollectorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MagnetMenu>> ITEM_MAGNET =
            MENUS.register("item_magnet", () -> IMenuTypeExtension.create(MagnetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarFurnaceMenu>> SOLAR_FURNACE =
            MENUS.register("solar_furnace", () -> IMenuTypeExtension.create(SolarFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FishnetMenu>> FISHNET =
            MENUS.register("fishnet", () -> IMenuTypeExtension.create(FishnetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SimpleBlockPlacerMenu>> SIMPLE_BLOCK_PLACER =
            MENUS.register("simple_block_placer", () -> IMenuTypeExtension.create(SimpleBlockPlacerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedBlockBreakerMenu>> ADVANCED_BLOCK_BREAKER =
            MENUS.register("advanced_block_breaker", () -> IMenuTypeExtension.create(AdvancedBlockBreakerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedBlockPlacerMenu>> ADVANCED_BLOCK_PLACER =
            MENUS.register("advanced_block_placer", () -> IMenuTypeExtension.create(AdvancedBlockPlacerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TransferNodeMenu>> TRANSFER_NODE =
            MENUS.register("transfer_node", () -> IMenuTypeExtension.create(TransferNodeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TransferEnergyMenu>> TRANSFER_NODE_ENERGY =
            MENUS.register("transfer_node_energy", () -> IMenuTypeExtension.create(TransferEnergyMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TransferFilterMenu>> TRANSFER_FILTER =
            MENUS.register("transfer_filter", () -> IMenuTypeExtension.create(TransferFilterMenu::new));

    private ModMenus() {}
}
