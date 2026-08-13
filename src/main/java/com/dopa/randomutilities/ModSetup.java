package com.dopa.randomutilities;

import com.dopa.randomutilities.filter.FilterNetwork;
import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.itemcollector.ItemCollectorNetwork;
import com.dopa.randomutilities.itemcollector.config.ItemCollectorConfig;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.machine.MachineNetwork;
import com.dopa.randomutilities.trashcan.TrashCanNetwork;
import com.dopa.randomutilities.redstoneclock.RedstoneClockNetwork;
import com.dopa.randomutilities.fishnet.FishnetNetwork;
import com.dopa.randomutilities.fishnet.config.FishnetConfig;
import com.dopa.randomutilities.fishnet.config.TreasureLootConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModCreativeTabs;
import com.dopa.randomutilities.registry.ModDataComponents;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.registry.ModTriggers;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

public final class ModSetup {
    private static final Identifier GENERATOR_RECIPES_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":generator_recipes");
    private static final Identifier DEV_NULL_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":devnull_config");
    private static final Identifier UPGRADE_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":upgrade_config");
    private static final Identifier ITEM_COLLECTOR_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":item_collector_config");
    private static final Identifier FISHNET_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":fishnet_config");
    private static final Identifier TREASURE_LOOT_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":treasure_loot_config");

    private ModSetup() {}

    public static void register(IEventBus modEventBus) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModTriggers.TRIGGER_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        // Load early so JEI (and other client plugins) see config values, not jar defaults only.
        TreasureLootConfig.load();
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    DevNullConfig.load();
                    UpgradeConfig.load();
                    ItemCollectorConfig.load();
                    FishnetConfig.load();
                    TreasureLootConfig.load();
                    GeneratorRecipeConfig.load();
                }));
        modEventBus.addListener(FilterNetwork::registerCapabilities);
        modEventBus.addListener(ModSetup::registerCapabilities);
        modEventBus.addListener(FilterNetwork::registerPayloads);
        modEventBus.addListener(MachineNetwork::registerPayloads);
        modEventBus.addListener(ItemCollectorNetwork::registerPayloads);
        modEventBus.addListener(TrashCanNetwork::registerPayloads);
        modEventBus.addListener(RedstoneClockNetwork::registerPayloads);
        modEventBus.addListener(FishnetNetwork::registerPayloads);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.MINI_CHEST.get(),
                (be, side) -> be.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.TRASH_CAN.get(),
                (be, side) -> be.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.SOLAR_FURNACE.get(),
                (be, side) -> be.itemHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.FISHNET.get(),
                (be, side) -> be.itemHandler(side)
        );
    }

    @EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
            event.addListener(
                    GENERATOR_RECIPES_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> {
                        GeneratorRecipeConfig.reload();
                        GeneratorRecipeConfig.rebuildBlockPools();
                    }
            );
            event.addListener(
                    DEV_NULL_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> DevNullConfig.reload()
            );
            event.addListener(
                    UPGRADE_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> UpgradeConfig.reload()
            );
            event.addListener(
                    ITEM_COLLECTOR_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> ItemCollectorConfig.reload()
            );
            event.addListener(
                    FISHNET_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> FishnetConfig.reload()
            );
            event.addListener(
                    TREASURE_LOOT_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> TreasureLootConfig.reload()
            );
        }

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            GeneratorRecipeConfig.rebuildBlockPools();
        }
    }
}
