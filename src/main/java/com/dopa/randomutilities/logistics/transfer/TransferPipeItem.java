package com.dopa.randomutilities.logistics.transfer;

import com.dopa.randomutilities.core.util.DescribedBlockItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TransferPipeItem extends DescribedBlockItem {
    public TransferPipeItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties, tooltipKey);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState clicked = level.getBlockState(clickedPos);
        boolean sneaking = context.isSecondaryUseActive();
        boolean node = clicked.getBlock() instanceof TransferNodeBlock;
        boolean hasPipe = node && clicked.getValue(TransferNodeBlock.HAS_PIPE);
        BlockPos toward = clickedPos.relative(face);
        BlockState towardState = level.getBlockState(toward);
        boolean towardNode = towardState.getBlock() instanceof TransferNodeBlock;
        boolean towardHasPipe = towardNode && towardState.getValue(TransferNodeBlock.HAS_PIPE);
        if (node && sneaking && !hasPipe) {
            return TransferNodeBlock.installPipe(
                    level, clickedPos, clicked, context.getPlayer(), context.getItemInHand());
        }
        // Standalone pipe OR pipe-bodied node: click a face toward an empty plate to fill that plate.
        if (towardNode && !towardHasPipe && (!node || hasPipe)) {
            return TransferNodeBlock.installPipe(
                    level, toward, towardState, context.getPlayer(), context.getItemInHand());
        }
        return super.useOn(context);
    }
}
