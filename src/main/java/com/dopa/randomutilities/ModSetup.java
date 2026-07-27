package com.dopa.randomutilities;

import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class ModSetup {
    private ModSetup() {}

    public static void register(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(ModSetup::onCommonSetup);
    }

    private static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(GeneratorRecipeConfig::load);
    }

    @EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
    public static class Events {
        @SubscribeEvent
        public static void addCreative(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                event.accept(ModItems.BASIC_STONE_GENERATOR);
            }
        }

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(new SimplePreparableReloadListener<Void>() {
                @Override
                protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                    return null;
                }

                @Override
                protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                    GeneratorRecipeConfig.reload();
                }
            });
        }
    }
}
