package com.dopa.randomutilities.transfer.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.transfer.TransferNodeBlock;
import com.dopa.randomutilities.transfer.TransferPipeBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.List;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class TransferClient {
    private TransferClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TRANSFER_NODE.get(), TransferNodeScreen::new);
        event.register(ModMenus.TRANSFER_NODE_ENERGY.get(), TransferEnergyScreen::new);
        event.register(ModMenus.TRANSFER_FILTER.get(), TransferFilterScreen::new);
    }

    @SubscribeEvent
    public static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "transfer_pipe_color"),
                TransferPipeItemTintSource.MAP_CODEC
        );
    }

    @SubscribeEvent
    public static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        for (var pipe : ModBlocks.pipes()) {
            TransferPipeBlock block = pipe.get();
            event.register(List.of(BlockTintSources.constant(block.channel().tint())), block);
        }
        event.register(List.of(TransferNodePipeTintSource.INSTANCE), ModBlocks.TRANSFER_NODE.get());
    }

    @SubscribeEvent
    public static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(TransferConnectionModel.ID, TransferConnectionModel.Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(new IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                return suppressDefaultBreak(state, level, pos);
            }

            @Override
            public boolean playBreakSound(BlockState state, Level level, BlockPos pos) {
                return suppressDefaultBreak(state, level, pos);
            }
        }, ModBlocks.TRANSFER_NODE.get());
    }

    private static boolean suppressDefaultBreak(BlockState state, Level level, BlockPos pos) {
        if (TransferNodeBlock.particleVisualFace(state) != null) {
            return false;
        }
        if (state.getValue(TransferNodeBlock.HAS_PIPE)) {
            return true;
        }
        var player = Minecraft.getInstance().player;
        return player != null && TransferNodeBlock.willStripHead(level, pos, state, player);
    }
}
