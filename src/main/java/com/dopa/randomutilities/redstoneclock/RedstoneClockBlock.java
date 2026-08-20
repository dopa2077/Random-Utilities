package com.dopa.randomutilities.redstoneclock;

import com.dopa.randomutilities.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RedstoneClockBlock extends BaseEntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    private static final MapCodec<RedstoneClockBlock> CODEC = simpleCodec(RedstoneClockBlock::new);
    private static final ThreadLocal<Boolean> UPDATING = ThreadLocal.withInitial(() -> false);
    /** When set, this clock position emits no redstone (used while reading control input). */
    private static final ThreadLocal<BlockPos> SIGNAL_MUTED_AT = new ThreadLocal<>();

    static boolean isUpdating() {
        return UPDATING.get();
    }

    /**
     * Strongest neighbor control signal, excluding this clock's own output
     * (avoids LOW/HIGH feedback through adjacent dust).
     */
    static int readControlSignal(Level level, BlockPos pos) {
        SIGNAL_MUTED_AT.set(pos.immutable());
        try {
            int best = 0;
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                int signal;
                if (neighborState.getBlock() instanceof RedStoneWireBlock wire) {
                    // Incoming power to the wire with this clock muted — ignores dust lit only by us.
                    signal = wire.getBlockSignal(level, neighborPos);
                } else {
                    signal = level.getSignal(neighborPos, direction);
                }
                if (signal >= 15) {
                    return 15;
                }
                if (signal > best) {
                    best = signal;
                }
            }
            return best;
        } finally {
            SIGNAL_MUTED_AT.remove();
        }
    }

    public RedstoneClockBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RUNNING, false)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends RedstoneClockBlock> codec() {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNNING, POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneClockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.REDSTONE_CLOCK.get(), RedstoneClockBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof RedstoneClockBlockEntity be)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new RedstoneClockMenu(id, inv, be),
                        be.getDisplayName()
                ),
                buf -> buf.writeBlockPos(pos)
        );
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (pos.equals(SIGNAL_MUTED_AT.get())) {
            return 0;
        }
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston
    ) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RedstoneClockBlockEntity be) {
            be.onNeighborChanged();
        }
    }

    static void updateRunningPowered(Level level, BlockPos pos, BlockState state, boolean running, boolean powered) {
        if (UPDATING.get()) {
            return;
        }

        boolean poweredChanged = state.getValue(POWERED) != powered;
        boolean runningChanged = state.getValue(RUNNING) != running;
        if (!poweredChanged && !runningChanged) {
            return;
        }

        BlockState next = state;
        if (runningChanged) {
            next = next.setValue(RUNNING, running);
        }
        if (poweredChanged) {
            next = next.setValue(POWERED, powered);
        }

        UPDATING.set(true);
        try {
            // Clients only via setBlock; redstone neighbors notified once when power changes.
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            if (poweredChanged) {
                level.updateNeighborsAt(pos, next.getBlock());
            }
        } finally {
            UPDATING.set(false);
        }
    }
}
