package com.dopa.randomutilities.core.machine;

import com.dopa.randomutilities.core.util.BlockOrientations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/** Orientation (and optional TRIGGERED) shared by breaker/placer blocks. */
public abstract class OrientedEntityBlock extends BaseEntityBlock {
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockOrientations.ORIENTATION;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    protected OrientedEntityBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP);
        if (usesTriggered()) {
            state = state.setValue(TRIGGERED, false);
        }
        this.registerDefaultState(state);
    }

    protected boolean usesTriggered() {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BlockOrientations.placed(this.defaultBlockState(), context);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return BlockOrientations.rotate(state, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return BlockOrientations.mirror(state, mirror);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (usesTriggered()) {
            builder.add(ORIENTATION, TRIGGERED);
        } else {
            builder.add(ORIENTATION);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof BlockEntity be) {
            OwnableMachine.bindPlacer(be, placer);
        }
    }
}
