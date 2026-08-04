package com.dopa.randomutilities;

import com.dopa.randomutilities.config.DevNullConfig;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.UpgradeConfig;
import com.dopa.randomutilities.filteritem.FilterNetwork;
import com.dopa.randomutilities.machine.MachineNetwork;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModCreativeTabs;
import com.dopa.randomutilities.registry.ModDataComponents;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

public final class ModSetup {
    private static final Identifier GENERATOR_RECIPES_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":generator_recipes");
    private static final Identifier DEV_NULL_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":devnull_config");
    private static final Identifier UPGRADE_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":upgrade_config");

    private ModSetup() {}

    public static void register(IEventBus modEventBus) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    DevNullConfig.load();
                    UpgradeConfig.load();
                    GeneratorRecipeConfig.load();
                }));
        modEventBus.addListener(FilterNetwork::registerCapabilities);
        modEventBus.addListener(FilterNetwork::registerPayloads);
        modEventBus.addListener(MachineNetwork::registerPayloads);
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
        }

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            GeneratorRecipeConfig.rebuildBlockPools();
        }
    }
}
