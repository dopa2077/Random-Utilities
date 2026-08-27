package com.dopa.randomutilities.machine.fishnet;

import com.dopa.randomutilities.machine.fishnet.menu.FishnetMenu;
import com.dopa.randomutilities.core.machine.OwnableMachine;
import com.dopa.randomutilities.core.machine.MachineBlocks;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FishnetBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FishnetBlock> CODEC = simpleCodec(FishnetBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Matches Blockbench basket: floor, walls, and rim. Open top = facing direction. */
    private static final VoxelShape SHAPE_UP = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            Block.box(0.0, 2.0, 14.0, 14.0, 14.0, 16.0),
            Block.box(0.0, 2.0, 0.0, 2.0, 14.0, 14.0),
            Block.box(2.0, 2.0, 0.0, 16.0, 14.0, 2.0),
            Block.box(14.0, 2.0, 2.0, 16.0, 14.0, 16.0),
            Block.box(2.0, 14.0, 14.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 14.0, 0.0, 14.0, 16.0, 2.0),
            Block.box(0.0, 14.0, 2.0, 2.0, 16.0, 16.0),
            Block.box(14.0, 14.0, 0.0, 16.0, 16.0, 14.0)
    );
    private static final VoxelShape SHAPE_NORTH = rotateX(SHAPE_UP);
    private static final VoxelShape SHAPE_EAST = rotateY(SHAPE_NORTH);
    private static final VoxelShape SHAPE_SOUTH = rotateY(SHAPE_EAST);
    private static final VoxelShape SHAPE_WEST = rotateY(SHAPE_SOUTH);
    private static final VoxelShape SHAPE_DOWN = rotateX(SHAPE_NORTH);

    public FishnetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends FishnetBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            case UP -> SHAPE_UP;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        // Open top faces the player (opposite of look direction), same idea as end rod facing.
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(WATERLOGGED, fluid.is(Fluids.WATER));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(
                state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Matches blockstate {@code "x": 90} model rotation. */
    private static VoxelShape rotateX(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                result[0] = Shapes.or(result[0], Shapes.box(minX, minZ, 1.0 - maxY, maxX, maxZ, 1.0 - minY)));
        return result[0];
    }

    /** Matches blockstate {@code "y": 90} model rotation. */
    private static VoxelShape rotateY(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                result[0] = Shapes.or(result[0], Shapes.box(1.0 - maxZ, minY, minX, 1.0 - minZ, maxY, maxX)));
        return result[0];
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof BlockEntity be) {
            OwnableMachine.bindPlacer(be, placer);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FishnetBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        // Bucket waterlog / drain like stairs (GUI must not steal these clicks).
        if (stack.is(Items.WATER_BUCKET) && !state.getValue(WATERLOGGED)) {
            if (!level.isClientSide()) {
                this.placeLiquid(level, pos, state, Fluids.WATER.getSource(false));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
                if (!player.hasInfiniteMaterials()) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BUCKET) && state.getValue(WATERLOGGED)) {
            if (!level.isClientSide()) {
                ItemStack taken = this.pickupBlock(player, level, pos, state);
                if (!taken.isEmpty()) {
                    this.getPickupSound(state).ifPresent(sound ->
                            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F));
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                    if (!player.hasInfiniteMaterials()) {
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, taken));
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof FishnetBlockEntity be)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return MachineBlocks.tryInsertUpgrade(player, stack, level, pos, be.upgrades());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FishnetBlockEntity be)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new FishnetMenu(id, inv, be),
                        state.getBlock().getName()
                ),
                buf -> buf.writeBlockPos(pos)
        );
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.FISHNET.get(), FishnetBlockEntity::serverTick);
    }
}
