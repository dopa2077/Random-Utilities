package com.dopa.randomutilities;

import java.util.Arrays;
import java.util.List;

import com.dopa.randomutilities.fishnet.client.FishnetRenderer;
import com.dopa.randomutilities.minichest.client.MiniChestRenderer;
import com.dopa.randomutilities.generator.client.ResourceGeneratorRenderer;
import com.dopa.randomutilities.generator.config.GeneratorType;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModEntities;
import com.dopa.randomutilities.tinytnt.client.PrimedTinyTntRenderer;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@Mod(value = dOPasRandomUtilities.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public class dOPasRandomUtilitiesClient {
    public static final int CREATIVE_PURPLE_TINT = 0xFFC070FF;
    public static final int WATER_TINT = 0xFF3F76E4;

    public dOPasRandomUtilitiesClient(ModContainer container) {}

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
