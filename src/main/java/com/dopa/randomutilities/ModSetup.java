package com.dopa.randomutilities;

import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModCreativeTabs;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class ModSetup {
    private static final Identifier GENERATOR_RECIPES_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":generator_recipes");

    private ModSetup() {}

    public static void register(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModSetup::onCommonSetup);
    }

    private static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(GeneratorRecipeConfig::load);
    }

    @EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
    public static class Events {
        @SubscribeEvent
        public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
            event.addListener(
                    GENERATOR_RECIPES_LISTENER,
                    (ResourceManagerReloadListener) (resourceManager -> GeneratorRecipeConfig.reload())
            );
        }
    }
}
