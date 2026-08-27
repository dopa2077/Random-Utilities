package com.dopa.randomutilities.logistics.transfer.client;

import com.dopa.randomutilities.logistics.transfer.TransferChannel;
import com.dopa.randomutilities.logistics.transfer.TransferNodeBlockEntity;
import com.dopa.randomutilities.logistics.transfer.TransferPipeBlock;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class TransferNodePipeTintSource implements BlockTintSource {
    public static final TransferNodePipeTintSource INSTANCE = new TransferNodePipeTintSource();

    private TransferNodePipeTintSource() {}

    @Override
    public int color(BlockState state) {
        return TransferPipeBlock.channel(state).tint();
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        Integer ordinal = level.getModelData(pos).get(TransferNodeBlockEntity.PIPE_CHANNEL);
        if (ordinal != null && ordinal >= 0 && ordinal < TransferChannel.values().length) {
            return TransferChannel.values()[ordinal].tint();
        }
        return color(state);
    }
}
