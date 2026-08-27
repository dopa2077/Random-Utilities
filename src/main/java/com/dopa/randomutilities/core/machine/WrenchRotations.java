package com.dopa.randomutilities.core.machine;

import com.dopa.randomutilities.core.util.BlockOrientations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * AE2-style wrench rotation for this mod's facing / front-and-top machines.
 * Solar panels, pipes, and transfer nodes have connection state rather than a
 * single front, so they never match these properties.
 */
public final class WrenchRotations {
    private WrenchRotations() {}

    public static boolean isRotatable(BlockState state) {
        return state.hasProperty(BlockOrientations.ORIENTATION)
                || state.hasProperty(BlockStateProperties.FACING)
                || state.hasProperty(HorizontalDirectionalBlock.FACING);
    }

    /**
     * Applies one AE2-style rotation. Returns true only if the blockstate actually changed.
     */
    public static boolean tryRotate(Level level, BlockPos pos, BlockState state, Direction clickedFace) {
        BlockState next = rotated(state, clickedFace);
        if (next == null || next == state) {
            return false;
        }
        return level.setBlock(pos, next, Block.UPDATE_ALL);
    }

    public static BlockState rotated(BlockState state, Direction clickedFace) {
        if (!isRotatable(state)) {
            return state;
        }
        if (state.hasProperty(BlockOrientations.ORIENTATION)) {
            return rotateFrontAndTop(state, clickedFace);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return rotateFullFacing(state, clickedFace);
        }
        return rotateHorizontal(state, clickedFace);
    }

    private static BlockState rotateFrontAndTop(BlockState state, Direction clicked) {
        EnumProperty<FrontAndTop> property = BlockOrientations.ORIENTATION;
        FrontAndTop current = state.getValue(property);
        Direction front = current.front();
        Direction top = current.top();
        FrontAndTop next;
        if (clicked == front || clicked == front.getOpposite()) {
            next = frontAndTop(front, top.getClockWise(front.getAxis()));
        } else {
            Direction newTop = top.getAxis() == clicked.getAxis()
                    ? defaultTop(clicked)
                    : top;
            next = frontAndTop(clicked, newTop);
        }
        return next == current ? state : state.setValue(property, next);
    }

    private static BlockState rotateFullFacing(BlockState state, Direction clicked) {
        Direction front = state.getValue(BlockStateProperties.FACING);
        Direction next;
        if (clicked == front || clicked == front.getOpposite()) {
            next = front.getClockWise(spinAxis(front));
        } else {
            next = clicked;
        }
        return next == front ? state : state.setValue(BlockStateProperties.FACING, next);
    }

    private static BlockState rotateHorizontal(BlockState state, Direction clicked) {
        Direction front = state.getValue(HorizontalDirectionalBlock.FACING);
        Direction next;
        if (!clicked.getAxis().isHorizontal() || clicked == front || clicked == front.getOpposite()) {
            next = front.getClockWise();
        } else {
            next = clicked;
        }
        return next == front ? state : state.setValue(HorizontalDirectionalBlock.FACING, next);
    }

    private static FrontAndTop frontAndTop(Direction front, Direction top) {
        if (top.getAxis() == front.getAxis()) {
            top = defaultTop(front);
        }
        return FrontAndTop.fromFrontAndTop(front, top);
    }

    private static Direction defaultTop(Direction front) {
        return front.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
    }

    private static Direction.Axis spinAxis(Direction front) {
        return front.getAxis().isVertical() ? Direction.Axis.Y : front.getAxis();
    }
}
