package com.dopa.randomutilities.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.registry.ModTags;
import com.dopa.randomutilities.transfer.menu.TransferEnergyMenu;
import com.dopa.randomutilities.transfer.menu.TransferNodeMenu;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TransferNodeBlock extends BaseEntityBlock {
    public static final BooleanProperty HAS_PIPE = BooleanProperty.create("has_pipe");
    public static final EnumProperty<TransferNodeFace> NORTH =
            EnumProperty.create("north", TransferNodeFace.class);
    public static final EnumProperty<TransferNodeFace> EAST =
            EnumProperty.create("east", TransferNodeFace.class);
    public static final EnumProperty<TransferNodeFace> SOUTH =
            EnumProperty.create("south", TransferNodeFace.class);
    public static final EnumProperty<TransferNodeFace> WEST =
            EnumProperty.create("west", TransferNodeFace.class);
    public static final EnumProperty<TransferNodeFace> UP =
            EnumProperty.create("up", TransferNodeFace.class);
    public static final EnumProperty<TransferNodeFace> DOWN =
            EnumProperty.create("down", TransferNodeFace.class);
    public static final Map<Direction, EnumProperty<TransferNodeFace>> FACE_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN
    );
    private static final int FACE_COUNT = TransferNodeFace.values().length;
    private static final Map<Integer, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();
    private static final MapCodec<TransferNodeBlock> CODEC = simpleCodec(TransferNodeBlock::new);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 1.0),
            Block.box(2.0, 2.0, 1.0, 14.0, 14.0, 2.0),
            Block.box(4.0, 4.0, 2.0, 12.0, 12.0, 4.0)
    );
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(1.0, 1.0, 15.0, 15.0, 15.0, 16.0),
            Block.box(2.0, 2.0, 14.0, 14.0, 14.0, 15.0),
            Block.box(4.0, 4.0, 12.0, 12.0, 12.0, 14.0)
    );
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(0.0, 1.0, 1.0, 1.0, 15.0, 15.0),
            Block.box(1.0, 2.0, 2.0, 2.0, 14.0, 14.0),
            Block.box(2.0, 4.0, 4.0, 4.0, 12.0, 12.0)
    );
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(15.0, 1.0, 1.0, 16.0, 15.0, 15.0),
            Block.box(14.0, 2.0, 2.0, 15.0, 14.0, 14.0),
            Block.box(12.0, 4.0, 4.0, 14.0, 12.0, 12.0)
    );
    private static final VoxelShape SHAPE_DOWN = Shapes.or(
            Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0),
            Block.box(2.0, 1.0, 2.0, 14.0, 2.0, 14.0),
            Block.box(4.0, 2.0, 4.0, 12.0, 4.0, 12.0)
    );
    private static final VoxelShape SHAPE_UP = Shapes.or(
            Block.box(1.0, 15.0, 1.0, 15.0, 16.0, 15.0),
            Block.box(2.0, 14.0, 2.0, 14.0, 15.0, 14.0),
            Block.box(4.0, 12.0, 4.0, 12.0, 14.0, 12.0)
    );

    public TransferNodeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_PIPE, false)
                .setValue(NORTH, TransferNodeFace.NONE)
                .setValue(EAST, TransferNodeFace.NONE)
                .setValue(SOUTH, TransferNodeFace.NONE)
                .setValue(WEST, TransferNodeFace.NONE)
                .setValue(UP, TransferNodeFace.NONE)
                .setValue(DOWN, TransferNodeFace.NONE));
    }

    @Override
    protected MapCodec<? extends TransferNodeBlock> codec() {
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

    public static boolean hasHead(BlockGetter level, BlockPos pos, Direction direction) {
        return TransferNodeBlockEntity.hasHead(headMask(level, pos), direction);
    }

    public static int headMask(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            return be.headMask();
        }
        Integer mask = level.getModelData(pos).get(TransferNodeBlockEntity.HEADS);
        return mask != null ? mask : 0;
    }

    public static TransferNodeFace face(BlockState state, Direction direction) {
        return state.getValue(FACE_BY_DIRECTION.get(direction));
    }

    static VoxelShape plateShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction particleFace = particleVisualFace(state);
        if (particleFace != null) {
            return plateShape(particleFace);
        }
        int heads = headMask(level, pos);
        int key = shapeIndex(state) << 6 | (heads & 63);
        return SHAPE_CACHE.computeIfAbsent(key, unused -> buildShape(state, heads));
    }

    @Nullable
    public static Direction particleVisualFace(BlockState state) {
        if (state.getValue(HAS_PIPE)) {
            return null;
        }
        Direction marked = null;
        for (Direction direction : Direction.values()) {
            if (face(state, direction) == TransferNodeFace.NONE) {
                continue;
            }
            if (marked != null) {
                return null;
            }
            marked = direction;
        }
        return marked;
    }

    private static int shapeIndex(BlockState state) {
        int index = state.getValue(HAS_PIPE) ? 1 : 0;
        for (Direction direction : Direction.values()) {
            index = index * FACE_COUNT + face(state, direction).ordinal();
        }
        return index;
    }

    private static VoxelShape buildShape(BlockState state, int heads) {
        VoxelShape shape = Shapes.empty();
        boolean hasPipe = state.getValue(HAS_PIPE);
        if (hasPipe) {
            shape = Shapes.or(shape, TransferPipeBlock.centerShape());
        }
        for (Direction direction : Direction.values()) {
            boolean headed = TransferNodeBlockEntity.hasHead(heads, direction);
            if (headed) {
                shape = Shapes.or(shape, plateShape(direction));
            }
            TransferNodeFace face = face(state, direction);
            if (hasPipe && headed) {
                shape = Shapes.or(shape, TransferPipeBlock.armShape(direction, true));
            } else if (hasPipe && (face == TransferNodeFace.PIPE || face == TransferNodeFace.INVENTORY)) {
                shape = Shapes.or(shape, TransferPipeBlock.armShape(
                        direction,
                        face != TransferNodeFace.PIPE
                ));
            }
            if (hasPipe && face == TransferNodeFace.INVENTORY && !headed) {
                shape = Shapes.or(shape, TransferPipeBlock.nozzleShape(direction));
            }
        }
        return shape.isEmpty() ? plateShape(Direction.NORTH) : shape;
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
        return withConnections(state, level, pos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState next = state;
        for (Direction direction : Direction.values()) {
            next = next.setValue(
                    FACE_BY_DIRECTION.get(rotation.rotate(direction)),
                    state.getValue(FACE_BY_DIRECTION.get(direction))
            );
        }
        return next;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState next = state;
        for (Direction direction : Direction.values()) {
            next = next.setValue(
                    FACE_BY_DIRECTION.get(mirror.mirror(direction)),
                    state.getValue(FACE_BY_DIRECTION.get(direction))
            );
        }
        return next;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_PIPE, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransferNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.TRANSFER_NODE.get(), TransferNodeBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (TransferNetworks.topologyChanged(oldState, state)) {
            TransferNetworks.invalidate(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        TransferNetworks.invalidate(level, pos);
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
        BlockState updated = withConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
            TransferNetworks.invalidate(level, pos);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (state.getValue(HAS_PIPE)) {
            TransferChannel channel = TransferChannel.NONE;
            if (be instanceof TransferNodeBlockEntity node) {
                channel = node.pipeChannel();
            }
            var pipeItem = ModItems.pipe(channel);
            if (pipeItem != null) {
                drops.add(new ItemStack(pipeItem.get()));
            }
        }
        int count = 0;
        if (be instanceof TransferNodeBlockEntity node) {
            for (Direction direction : Direction.values()) {
                if (!node.hasHead(direction)) {
                    continue;
                }
                var nodeItem = ModItems.node(node.head(direction).kind());
                if (nodeItem != null) {
                    drops.add(new ItemStack(nodeItem.get()));
                }
                count++;
            }
        }
        if (count <= 0 && ModItems.TRANSFER_NODE != null) {
            drops.add(new ItemStack(ModItems.TRANSFER_NODE.get()));
        }
        return drops;
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            ItemStack toolStack,
            boolean willHarvest,
            FluidState fluid
    ) {
        if (state.getValue(HAS_PIPE)) {
            stripPipe(level, pos, state, player, !player.getAbilities().instabuild, true);
            return false;
        }
        Direction head = targetedHead(level, pos, state, player);
        if (head != null && headCount(level, pos) > 1) {
            stripHead(level, pos, state, player, !player.getAbilities().instabuild, true, head);
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        if (state.getValue(HAS_PIPE) || willStripHead(level, pos, state, player)) {
            return;
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    public static boolean willStripHead(BlockGetter level, BlockPos pos, BlockState state, Player player) {
        return !state.getValue(HAS_PIPE)
                && targetedHead(level, pos, state, player) != null
                && headCount(level, pos) > 1;
    }

    static int headCount(BlockGetter level, BlockPos pos) {
        return Integer.bitCount(headMask(level, pos));
    }

    @Nullable
    static Direction targetedHead(BlockGetter level, BlockPos pos, BlockState state, Player player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos)) {
            return null;
        }
        Direction plate = headAt(level, pos, state, blockHit);
        if (plate != null) {
            return plate;
        }
        Direction face = blockHit.getDirection();
        return hasHead(level, pos, face) ? face : null;
    }

    static BlockState withConnections(BlockState state, LevelReader level, BlockPos pos) {
        boolean hasPipe = state.getValue(HAS_PIPE);
        int heads = headMask(level, pos);
        BlockState next = state;
        for (Direction direction : Direction.values()) {
            EnumProperty<TransferNodeFace> property = FACE_BY_DIRECTION.get(direction);
            if (TransferNodeBlockEntity.hasHead(heads, direction)) {
                next = next.setValue(property, TransferNodeFace.NONE);
                continue;
            }
            if (!hasPipe) {
                next = next.setValue(property, TransferNodeFace.NONE);
                continue;
            }
            TransferChannel from = hasPipe ? TransferPipeBlock.channel(level, pos, state) : TransferChannel.NONE;
            TransferPipeFace natural = TransferPipeBlock.detect(level, pos.relative(direction), direction, from);
            if (state.getValue(property) == TransferNodeFace.DISABLED
                    && TransferPipeBlock.keepsDisabled(natural)) {
                next = next.setValue(property, TransferNodeFace.DISABLED);
                continue;
            }
            TransferNodeFace face = switch (natural) {
                case PIPE -> TransferNodeFace.PIPE;
                case INVENTORY -> TransferNodeFace.INVENTORY;
                case DISABLED, NONE -> TransferNodeFace.NONE;
            };
            next = next.setValue(property, face);
        }
        return next;
    }

    static InteractionResult addHead(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction head,
            Player player,
            ItemStack stack
    ) {
        if (hasHead(level, pos, head)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be)) {
            return InteractionResult.FAIL;
        }
        HeadKind kind = stack.getItem() instanceof TransferNodeItem item ? item.kind() : HeadKind.ITEM;
        be.setHead(head, kind);
        BlockState updated = withConnections(
                state.setValue(FACE_BY_DIRECTION.get(head), TransferNodeFace.NONE),
                level,
                pos
        );
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 1.1F);
        TransferNetworks.invalidate(level, pos);
        return InteractionResult.CONSUME;
    }

    static InteractionResult convertPipe(
            Level level,
            BlockPos pos,
            BlockState pipeState,
            Direction head,
            Player player,
            ItemStack stack,
            BlockState nodeDefault
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockState node = nodeDefault.setValue(HAS_PIPE, true);
        for (Direction direction : Direction.values()) {
            TransferNodeFace face = switch (TransferPipeBlock.face(pipeState, direction)) {
                case INVENTORY -> TransferNodeFace.INVENTORY;
                case PIPE -> TransferNodeFace.PIPE;
                case DISABLED -> TransferNodeFace.DISABLED;
                case NONE -> TransferNodeFace.NONE;
            };
            node = node.setValue(FACE_BY_DIRECTION.get(direction), face);
        }
        node = node.setValue(FACE_BY_DIRECTION.get(head), TransferNodeFace.NONE);
        level.setBlock(pos, node, Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            HeadKind kind = stack.getItem() instanceof TransferNodeItem item ? item.kind() : HeadKind.ITEM;
            be.setHead(head, kind);
            be.setPipeChannel(TransferPipeBlock.channel(pipeState));
        }
        node = withConnections(level.getBlockState(pos), level, pos);
        level.setBlock(pos, node, Block.UPDATE_ALL);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 1.1F);
        TransferNetworks.invalidate(level, pos);
        return InteractionResult.CONSUME;
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
        boolean shift = player.isShiftKeyDown();
        boolean pipe = stack.is(ModTags.TRANSFER_PIPES);
        boolean hasPipe = state.getValue(HAS_PIPE);
        DyeColor dye = DyeColor.getColor(stack);
        if (dye != null && hasPipe) {
            TransferChannel next = TransferChannel.fromDye(dye);
            if (!(level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) || be.pipeChannel() == next) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            be.setPipeChannel(next);
            BlockState updated = withConnections(state, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
            TransferNetworks.invalidate(level, pos);
            return InteractionResult.CONSUME;
        }
        if (pipe && shift && !hasPipe) {
            return installPipe(level, pos, state, player, stack);
        }
        if (shift) {
            Direction face = headAt(level, pos, state, hit);
            if (face == null) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!(level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!TransferNodeUpgradeInventory.isNodeUpgrade(be.head(face).kind(), stack)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            int inserted = be.insertUpgrade(face, stack);
            if (inserted <= 0) {
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(inserted);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.1F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    static InteractionResult installPipe(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (state.getValue(HAS_PIPE)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            be.setPipeChannel(TransferPipeBlock.channel(stack));
        }
        BlockState installed = withConnections(state.setValue(HAS_PIPE, true), level, pos);
        level.setBlock(pos, installed, Block.UPDATE_ALL);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 1.1F);
        TransferNetworks.invalidate(level, pos);
        return InteractionResult.CONSUME;
    }

    private static void stripPipe(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Player player,
            boolean drop,
            boolean breakEffects
    ) {
        if (breakEffects) {
            spawnPipeBreakEffects(level, pos, state, player);
        }
        TransferChannel channel = TransferChannel.NONE;
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            channel = be.pipeChannel();
            be.setPipeChannel(TransferChannel.NONE);
        }
        BlockState removed = withConnections(state.setValue(HAS_PIPE, false), level, pos);
        level.setBlock(pos, removed, Block.UPDATE_ALL);
        if (!level.isClientSide() && drop) {
            var pipeItem = ModItems.pipe(channel);
            if (pipeItem != null) {
                popResource(level, pos, new ItemStack(pipeItem.get()));
            }
        }
        if (!level.isClientSide()) {
            TransferNetworks.invalidate(level, pos);
        }
    }

    private static void stripHead(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Player player,
            boolean drop,
            boolean breakEffects,
            Direction head
    ) {
        if (!(level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) || !be.hasHead(head)) {
            return;
        }
        if (breakEffects) {
            spawnHeadBreakEffects(level, pos, player, head);
        }
        HeadKind kind = be.head(head).kind();
        if (!level.isClientSide() && drop) {
            be.dropUpgrades(head);
        }
        be.setHead(head, false);
        BlockState updated = withConnections(level.getBlockState(pos), level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
        if (!level.isClientSide() && drop) {
            var nodeItem = ModItems.node(kind);
            if (nodeItem != null) {
                popResource(level, pos, new ItemStack(nodeItem.get()));
            }
        }
        if (!level.isClientSide()) {
            TransferNetworks.invalidate(level, pos);
        }
    }

    private static void spawnPipeBreakEffects(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Player player
    ) {
        int visual = Block.getId(pipeBreakVisual(state, headMask(level, pos), pipeChannel(level, pos)));
        if (level.isClientSide()) {
            level.levelEvent(2001, pos, visual);
        } else {
            level.levelEvent(player, 2001, pos, visual);
        }
    }

    private static void spawnHeadBreakEffects(
            Level level,
            BlockPos pos,
            @Nullable Player player,
            Direction head
    ) {
        int visual = Block.getId(headBreakVisual(head));
        if (level.isClientSide()) {
            level.levelEvent(2001, pos, visual);
        } else {
            level.levelEvent(player, 2001, pos, visual);
        }
    }

    static BlockState headBreakVisual(Direction head) {
        return ModBlocks.TRANSFER_NODE.get().defaultBlockState()
                .setValue(FACE_BY_DIRECTION.get(head), TransferNodeFace.DISABLED);
    }

    static int pipeChannelOrdinal(BlockGetter level, BlockPos pos) {
        return pipeChannel(level, pos).ordinal();
    }

    static TransferChannel pipeChannel(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            return be.pipeChannel();
        }
        return TransferChannel.NONE;
    }

    public static BlockState pipeBreakVisual(BlockState node, int heads) {
        return pipeBreakVisual(node, heads, TransferChannel.NONE);
    }

    public static BlockState pipeBreakVisual(BlockState node, int heads, TransferChannel channel) {
        var pipeBlock = ModBlocks.pipe(channel);
        if (pipeBlock == null) {
            return node;
        }
        BlockState pipe = pipeBlock.get().defaultBlockState();
        boolean hasPipe = node.getValue(HAS_PIPE);
        for (Direction direction : Direction.values()) {
            TransferNodeFace face = face(node, direction);
            TransferPipeFace kind = TransferPipeFace.NONE;
            if (hasPipe) {
                if (TransferNodeBlockEntity.hasHead(heads, direction) || face == TransferNodeFace.PIPE) {
                    kind = TransferPipeFace.PIPE;
                } else {
                    kind = switch (face) {
                        case INVENTORY -> TransferPipeFace.INVENTORY;
                        case DISABLED -> TransferPipeFace.DISABLED;
                        default -> TransferPipeFace.NONE;
                    };
                }
            }
            pipe = TransferPipeBlock.withConnection(pipe, direction, kind);
        }
        return pipe;
    }

    @Nullable
    static Direction headAt(BlockGetter level, BlockPos pos, BlockState state, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (!hasHead(level, pos, direction) || !TransferPipeBlock.contains(plateShape(direction), local)) {
                continue;
            }
            var bounds = plateShape(direction).bounds();
            double cx = (bounds.minX + bounds.maxX) * 0.5;
            double cy = (bounds.minY + bounds.maxY) * 0.5;
            double cz = (bounds.minZ + bounds.maxZ) * 0.5;
            double dist = local.distanceToSqr(cx, cy, cz);
            if (dist < bestDist) {
                bestDist = dist;
                best = direction;
            }
        }
        return best;
    }

    @Nullable
    static Direction pipeArmAt(BlockGetter level, BlockPos pos, BlockState state, BlockHitResult hit) {
        if (!state.getValue(HAS_PIPE)) {
            return null;
        }
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (hasHead(level, pos, direction)) {
                continue;
            }
            TransferNodeFace face = face(state, direction);
            if (face == TransferNodeFace.NONE) {
                continue;
            }
            VoxelShape shape = TransferPipeBlock.armShape(direction, face != TransferNodeFace.PIPE);
            if (face == TransferNodeFace.INVENTORY) {
                shape = Shapes.or(shape, TransferPipeBlock.nozzleShape(direction));
            }
            if (!TransferPipeBlock.contains(shape, local)) {
                continue;
            }
            var bounds = shape.bounds();
            double dist = local.distanceToSqr(
                    (bounds.minX + bounds.maxX) * 0.5,
                    (bounds.minY + bounds.maxY) * 0.5,
                    (bounds.minZ + bounds.maxZ) * 0.5
            );
            if (dist < bestDist) {
                bestDist = dist;
                best = direction;
            }
        }
        return best;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (player.isShiftKeyDown() && state.getValue(HAS_PIPE) && headAt(level, pos, state, hit) == null) {
            Direction arm = pipeArmAt(level, pos, state, hit);
            if (arm == null) {
                arm = hit.getDirection();
            }
            InteractionResult toggled = TransferPipeBlock.toggleLink(level, pos, state, arm, player);
            if (toggled.consumesAction()) {
                return toggled;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            stripPipe(level, pos, state, player, player == null || !player.getAbilities().instabuild, false);
            return InteractionResult.CONSUME;
        }
        Direction face = headAt(level, pos, state, hit);
        if (face == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        HeadKind kind = be.head(face).kind();
        if (kind == HeadKind.ENERGY) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, p) -> new TransferEnergyMenu(id, inv, be, face),
                            Component.translatable("container.dopasrandomutilities.transfer_node_energy")
                    ),
                    buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeByte(face.get3DDataValue());
                    }
            );
            return InteractionResult.CONSUME;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new TransferNodeMenu(id, inv, be, face),
                        Component.translatable(kind == HeadKind.FLUID
                                ? "container.dopasrandomutilities.transfer_node_fluid"
                                : "container.dopasrandomutilities.transfer_node")
                ),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(face.get3DDataValue());
                }
        );
        return InteractionResult.CONSUME;
    }
}
