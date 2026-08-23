package com.dopa.randomutilities.tinytnt;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
public final class TinyTntEvents {
    private TinyTntEvents() {}

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof PrimedTinyTnt)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel server)) {
            return;
        }

        List<BlockPos> logs = new ArrayList<>();
        event.getAffectedBlocks().removeIf(pos -> {
            if (!server.getBlockState(pos).is(BlockTags.LOGS)) {
                return false;
            }
            logs.add(pos.immutable());
            return true;
        });

        for (BlockPos pos : logs) {
            BlockState state = server.getBlockState(pos);
            if (!state.is(BlockTags.LOGS)) {
                continue;
            }
            server.removeBlock(pos, false);
            Block.popResource(
                    server,
                    pos,
                    new ItemStack(ModItems.WOOD_CHIP.get(), PrimedTinyTnt.rollWoodChips(server.getRandom()))
            );
        }
    }
}
