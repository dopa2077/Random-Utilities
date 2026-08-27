package com.dopa.randomutilities.core.util;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** 12-way placement: front can point any direction, and vertical fronts rotate toward the player. */
public final class BlockOrientations {
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

    private BlockOrientations() {}

    public static Direction front(BlockState state) {
        return state.getValue(ORIENTATION).front();
    }

    public static BlockState placed(BlockState state, BlockPlaceContext context) {
        Direction front = context.getNearestLookingDirection().getOpposite();
        Direction top = switch (front) {
            case DOWN -> context.getHorizontalDirection().getOpposite();
            case UP -> context.getHorizontalDirection();
            default -> Direction.UP;
        };
        return state.setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(front, top));
    }

    public static BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ORIENTATION, rotation.rotation().rotate(state.getValue(ORIENTATION)));
    }

    public static BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ORIENTATION, mirror.rotation().rotate(state.getValue(ORIENTATION)));
    }
}
