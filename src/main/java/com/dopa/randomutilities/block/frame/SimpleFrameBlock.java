package com.dopa.randomutilities.block.frame;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Decorative cutout frame that can sit on a redstone line. */
public class SimpleFrameBlock extends Block {
    public static final MapCodec<SimpleFrameBlock> CODEC = simpleCodec(SimpleFrameBlock::new);

    public SimpleFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleFrameBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, side);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
