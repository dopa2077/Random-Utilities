package com.dopa.randomutilities;

import java.util.List;

import com.dopa.randomutilities.client.ResourceGeneratorRenderer;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;

import net.minecraft.client.color.block.BlockTintSources;
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
    /** Multiply tint for creative generators (ARGB). */
    public static final int CREATIVE_PURPLE_TINT = 0xFFC070FF;

    /** Default vanilla water multiply tint (ARGB). Water faces use tintindex 1. */
    public static final int WATER_TINT = 0xFF3F76E4;

    public dOPasRandomUtilitiesClient(ModContainer container) {
        // No common ModConfigSpec — generator behaviour lives in config JSON files.
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.RESOURCE_GENERATOR.get(),
                ResourceGeneratorRenderer::new
        );
    }

    @SubscribeEvent
    static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        // tintindex 0 = casing (creative purple or unused), tintindex 1 = water
        event.register(
                List.of(BlockTintSources.constant(-1), BlockTintSources.constant(WATER_TINT)),
                ModBlocks.BASIC_STONE_GENERATOR.get(),
                ModBlocks.INTERMEDIATE_STONE_GENERATOR.get(),
                ModBlocks.ADVANCED_STONE_GENERATOR.get(),
                ModBlocks.ELITE_STONE_GENERATOR.get(),
                ModBlocks.ULTIMATE_STONE_GENERATOR.get(),
                ModBlocks.RANDOM_ORE_GENERATOR.get(),
                ModBlocks.METAL_BLOCK_GENERATOR.get(),
                ModBlocks.CREATIVE_RANDOM_ORE_GENERATOR.get(),
                ModBlocks.CREATIVE_METAL_BLOCK_GENERATOR.get()
        );
        // Ore/metal creative generators use baked creative textures in their models.
        // Stone creative still relies on multiply tint for casing.
        event.register(
                List.of(BlockTintSources.constant(CREATIVE_PURPLE_TINT), BlockTintSources.constant(WATER_TINT)),
                ModBlocks.CREATIVE_STONE_GENERATOR.get()
        );
    }
}
