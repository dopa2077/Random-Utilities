package com.dopa.randomutilities;

import java.util.Arrays;
import java.util.List;

import com.dopa.randomutilities.machine.breaker.client.AdvancedBlockBreakerScreen;
import com.dopa.randomutilities.machine.placer.client.AdvancedBlockPlacerScreen;
import com.dopa.randomutilities.machine.placer.client.SimpleBlockPlacerScreen;
import com.dopa.randomutilities.machine.combustion.client.CombustionGeneratorScreen;
import com.dopa.randomutilities.core.filter.client.FilterScreen;
import com.dopa.randomutilities.machine.fishnet.client.FishnetRenderer;
import com.dopa.randomutilities.machine.fishnet.client.FishnetScreen;
import com.dopa.randomutilities.machine.generator.client.ResourceGeneratorRenderer;
import com.dopa.randomutilities.machine.generator.client.ResourceGeneratorScreen;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.logistics.collector.client.ItemCollectorScreen;
import com.dopa.randomutilities.item.magnet.client.MagnetScreen;
import com.dopa.randomutilities.block.minichest.client.MiniChestRenderer;
import com.dopa.randomutilities.block.minichest.client.MiniChestScreen;
import com.dopa.randomutilities.block.redstoneclock.client.RedstoneClockScreen;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModEntities;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.machine.solar.furnace.client.SolarFurnaceScreen;
import com.dopa.randomutilities.machine.solar.panel.client.SolarPanelControllerScreen;
import com.dopa.randomutilities.block.tinytnt.client.PrimedTinyTntRenderer;
import com.dopa.randomutilities.logistics.transfer.client.TransferEnergyScreen;
import com.dopa.randomutilities.logistics.transfer.client.TransferFilterScreen;
import com.dopa.randomutilities.logistics.transfer.client.TransferNodeScreen;
import com.dopa.randomutilities.block.trashcan.client.TrashCanScreen;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = dOPasRandomUtilities.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public class dOPasRandomUtilitiesClient {
    public static final int CREATIVE_PURPLE_TINT = 0xFFC070FF;
    public static final int WATER_TINT = 0xFF3F76E4;

    public dOPasRandomUtilitiesClient(ModContainer container) {}

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FILTER.get(), FilterScreen::new);
        event.register(ModMenus.RESOURCE_GENERATOR.get(), ResourceGeneratorScreen::new);
        event.register(ModMenus.MINI_CHEST.get(), MiniChestScreen::new);
        event.register(ModMenus.TRASH_CAN.get(), TrashCanScreen::new);
        event.register(ModMenus.REDSTONE_CLOCK.get(), RedstoneClockScreen::new);
        event.register(ModMenus.ITEM_COLLECTOR.get(), ItemCollectorScreen::new);
        event.register(ModMenus.ITEM_MAGNET.get(), MagnetScreen::new);
        event.register(ModMenus.SOLAR_FURNACE.get(), SolarFurnaceScreen::new);
        event.register(ModMenus.FISHNET.get(), FishnetScreen::new);
        event.register(ModMenus.SIMPLE_BLOCK_PLACER.get(), SimpleBlockPlacerScreen::new);
        event.register(ModMenus.ADVANCED_BLOCK_BREAKER.get(), AdvancedBlockBreakerScreen::new);
        event.register(ModMenus.ADVANCED_BLOCK_PLACER.get(), AdvancedBlockPlacerScreen::new);
        event.register(ModMenus.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenus.SOLAR_PANEL_CONTROLLER.get(), SolarPanelControllerScreen::new);
        event.register(ModMenus.TRANSFER_NODE.get(), TransferNodeScreen::new);
        event.register(ModMenus.TRANSFER_NODE_ENERGY.get(), TransferEnergyScreen::new);
        event.register(ModMenus.TRANSFER_FILTER.get(), TransferFilterScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (ModBlockEntities.RESOURCE_GENERATOR != null) {
            event.registerBlockEntityRenderer(
                    ModBlockEntities.RESOURCE_GENERATOR.get(),
                    ResourceGeneratorRenderer::new
            );
        }
        if (ModBlockEntities.MINI_CHEST != null) {
            event.registerBlockEntityRenderer(
                    ModBlockEntities.MINI_CHEST.get(),
                    MiniChestRenderer::new
            );
        }
        if (ModBlockEntities.FISHNET != null) {
            event.registerBlockEntityRenderer(
                    ModBlockEntities.FISHNET.get(),
                    FishnetRenderer::new
            );
        }
        if (ModEntities.PRIMED_TINY_TNT != null) {
            event.registerEntityRenderer(ModEntities.PRIMED_TINY_TNT.get(), PrimedTinyTntRenderer::new);
        }
    }

    @SubscribeEvent
    static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        Block[] standard = Arrays.stream(GeneratorType.values())
                .filter(type -> type != GeneratorType.CREATIVE_STONE)
                .map(ModBlocks::forType)
                .filter(block -> block != null)
                .map(block -> block.get())
                .toArray(Block[]::new);
        if (standard.length > 0) {
            event.register(
                    List.of(BlockTintSources.constant(-1), BlockTintSources.constant(WATER_TINT)),
                    standard
            );
        }
        var creative = ModBlocks.forType(GeneratorType.CREATIVE_STONE);
        if (creative != null) {
            event.register(
                    List.of(BlockTintSources.constant(CREATIVE_PURPLE_TINT), BlockTintSources.constant(WATER_TINT)),
                    creative.get()
            );
        }
    }
}
