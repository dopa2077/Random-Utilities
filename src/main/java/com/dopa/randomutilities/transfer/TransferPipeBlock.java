package com.dopa.randomutilities.transfer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dopa.randomutilities.registry.ModBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

public class TransferPipeBlock extends Block {
    public static final EnumProperty<TransferPipeFace> NORTH =
            EnumProperty.create("north", TransferPipeFace.class);
    public static final EnumProperty<TransferPipeFace> EAST =
            EnumProperty.create("east", TransferPipeFace.class);
    public static final EnumProperty<TransferPipeFace> SOUTH =
            EnumProperty.create("south", TransferPipeFace.class);
    public static final EnumProperty<TransferPipeFace> WEST =
            EnumProperty.create("west", TransferPipeFace.class);
    public static final EnumProperty<TransferPipeFace> UP =
            EnumProperty.create("up", TransferPipeFace.class);
    public static final EnumProperty<TransferPipeFace> DOWN =
            EnumProperty.create("down", TransferPipeFace.class);

    public static final Map<Direction, EnumProperty<TransferPipeFace>> FACE_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN
    );

    private static final VoxelShape CENTER = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape ARM_NORTH = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 6.0);
    private static final VoxelShape ARM_SOUTH = Block.box(6.0, 6.0, 10.0, 10.0, 10.0, 16.0);
    private static final VoxelShape ARM_WEST = Block.box(0.0, 6.0, 6.0, 6.0, 10.0, 10.0);
    private static final VoxelShape ARM_EAST = Block.box(10.0, 6.0, 6.0, 16.0, 10.0, 10.0);
    private static final VoxelShape ARM_DOWN = Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);
    private static final VoxelShape ARM_UP = Block.box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0);
    private static final VoxelShape ARM_SHORT_NORTH = Block.box(6.0, 6.0, 1.0, 10.0, 10.0, 6.0);
    private static final VoxelShape ARM_SHORT_SOUTH = Block.box(6.0, 6.0, 10.0, 10.0, 10.0, 15.0);
    private static final VoxelShape ARM_SHORT_WEST = Block.box(1.0, 6.0, 6.0, 6.0, 10.0, 10.0);
    private static final VoxelShape ARM_SHORT_EAST = Block.box(10.0, 6.0, 6.0, 15.0, 10.0, 10.0);
    private static final VoxelShape ARM_SHORT_DOWN = Block.box(6.0, 1.0, 6.0, 10.0, 6.0, 10.0);
    private static final VoxelShape ARM_SHORT_UP = Block.box(6.0, 10.0, 6.0, 10.0, 15.0, 10.0);
    private static final VoxelShape NOZZLE_NORTH = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 3.0);
    private static final VoxelShape NOZZLE_SOUTH = Block.box(5.0, 5.0, 13.0, 11.0, 11.0, 16.0);
    private static final VoxelShape NOZZLE_WEST = Block.box(0.0, 5.0, 5.0, 3.0, 11.0, 11.0);
    private static final VoxelShape NOZZLE_EAST = Block.box(13.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape NOZZLE_DOWN = Block.box(5.0, 0.0, 5.0, 11.0, 3.0, 11.0);
    private static final VoxelShape NOZZLE_UP = Block.box(5.0, 13.0, 5.0, 11.0, 16.0, 11.0);
    private static final int FACE_COUNT = TransferPipeFace.values().length;
    private static final Map<Integer, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();

    private final TransferChannel channel;
    private final MapCodec<TransferPipeBlock> codec;

    public TransferPipeBlock(Properties properties) {
        this(properties, TransferChannel.NONE);
    }

    public TransferPipeBlock(Properties properties, TransferChannel channel) {
        super(properties);
        this.channel = channel;
        this.codec = simpleCodec(props -> new TransferPipeBlock(props, channel));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, TransferPipeFace.NONE)
                .setValue(EAST, TransferPipeFace.NONE)
                .setValue(SOUTH, TransferPipeFace.NONE)
                .setValue(WEST, TransferPipeFace.NONE)
                .setValue(UP, TransferPipeFace.NONE)
                .setValue(DOWN, TransferPipeFace.NONE));
    }

    @Override
    protected MapCodec<? extends TransferPipeBlock> codec() {
        return codec;
    }

    public TransferChannel channel() {
        return channel;
    }

    public static TransferChannel channel(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof TransferPipeBlock pipe) {
            return pipe.channel();
        }
        return TransferChannel.NONE;
    }

    public static TransferChannel channel(BlockState state) {
        return state.getBlock() instanceof TransferPipeBlock pipe ? pipe.channel() : TransferChannel.NONE;
    }

    public static TransferChannel channel(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof TransferPipeBlock pipe) {
            return pipe.channel();
        }
        if (state.getBlock() instanceof TransferNodeBlock
                && state.getValue(TransferNodeBlock.HAS_PIPE)
                && level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            return be.pipeChannel();
        }
        return TransferChannel.NONE;
    }

    public static TransferPipeFace face(BlockState state, Direction direction) {
        return state.getValue(FACE_BY_DIRECTION.get(direction));
    }

    static VoxelShape centerShape() {
        return CENTER;
    }

    static VoxelShape armShape(Direction direction, boolean shortened) {
        return switch (direction) {
            case NORTH -> shortened ? ARM_SHORT_NORTH : ARM_NORTH;
            case SOUTH -> shortened ? ARM_SHORT_SOUTH : ARM_SOUTH;
            case WEST -> shortened ? ARM_SHORT_WEST : ARM_WEST;
            case EAST -> shortened ? ARM_SHORT_EAST : ARM_EAST;
            case UP -> shortened ? ARM_SHORT_UP : ARM_UP;
            case DOWN -> shortened ? ARM_SHORT_DOWN : ARM_DOWN;
        };
    }

    static VoxelShape nozzleShape(Direction direction) {
        return switch (direction) {
            case NORTH -> NOZZLE_NORTH;
            case SOUTH -> NOZZLE_SOUTH;
            case WEST -> NOZZLE_WEST;
            case EAST -> NOZZLE_EAST;
            case UP -> NOZZLE_UP;
            case DOWN -> NOZZLE_DOWN;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int faceKey = shapeIndex(state);
        // Only probe neighbors for short arms when this pipe has PIPE faces.
        int shorts = needsShortProbe(state) ? shortMask(level, pos, state) : 0;
        int key = faceKey | shorts << 12;
        return SHAPE_CACHE.computeIfAbsent(key, unused -> buildShape(state, shorts));
    }

    private static boolean needsShortProbe(BlockState state) {
        for (Direction direction : Direction.values()) {
            if (face(state, direction) == TransferPipeFace.PIPE) {
                return true;
            }
        }
        return false;
    }

    public static boolean towardHead(BlockGetter level, BlockPos pos, Direction direction) {
        return TransferNodeBlock.hasHead(level, pos.relative(direction), direction.getOpposite());
    }

    private static int shortMask(BlockGetter level, BlockPos pos, BlockState state) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (face(state, direction) == TransferPipeFace.PIPE && towardHead(level, pos, direction)) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    private static int shapeIndex(BlockState state) {
        int index = 0;
        for (Direction direction : Direction.values()) {
            index = index * FACE_COUNT + face(state, direction).ordinal();
        }
        return index;
    }

    private static VoxelShape buildShape(BlockState state, int shorts) {
        VoxelShape shape = CENTER;
        for (Direction direction : Direction.values()) {
            TransferPipeFace face = face(state, direction);
            if (!face.hasArm()) {
                continue;
            }
            boolean shortened = face.shortened() || (shorts & (1 << direction.ordinal())) != 0;
            shape = Shapes.or(shape, armShape(direction, shortened));
            if (face == TransferPipeFace.INVENTORY) {
                shape = Shapes.or(shape, nozzleShape(direction));
            }
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
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
        }
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
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Direction arm = armAt(state, level, pos, hit);
        if (arm == null) {
            arm = hit.getDirection();
        }
        return toggleLink(level, pos, state, arm, player);
    }

    static BlockState withConnections(BlockState state, LevelReader level, BlockPos pos) {
        BlockState next = state;
        for (Direction direction : Direction.values()) {
            TransferPipeFace current = face(state, direction);
            TransferPipeFace natural = detect(level, pos.relative(direction), direction, channel(state));
            next = next.setValue(
                    FACE_BY_DIRECTION.get(direction),
                    current == TransferPipeFace.DISABLED && keepsDisabled(natural)
                            ? TransferPipeFace.DISABLED
                            : natural
            );
        }
        return next;
    }

    static BlockState withConnection(BlockState state, Direction direction, TransferPipeFace face) {
        return state.setValue(FACE_BY_DIRECTION.get(direction), face);
    }

    static TransferPipeFace detect(LevelReader level, BlockPos neighborPos, Direction towardNeighbor) {
        return detect(level, neighborPos, towardNeighbor, TransferChannel.NONE);
    }

    static TransferPipeFace detect(
            LevelReader level,
            BlockPos neighborPos,
            Direction towardNeighbor,
            TransferChannel from
    ) {
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof TransferNodeBlock
                && TransferNodeBlock.hasHead(level, neighborPos, towardNeighbor.getOpposite())) {
            return TransferPipeFace.PIPE;
        }
        if (TransferNetworks.isPipeHub(neighbor)) {
            TransferChannel to = channel(level, neighborPos, neighbor);
            return from.connectsTo(to) ? TransferPipeFace.PIPE : TransferPipeFace.NONE;
        }
        if (level instanceof Level world) {
            Direction insert = towardNeighbor.getOpposite();
            if (world.getCapability(Capabilities.Item.BLOCK, neighborPos, insert) != null
                    || world.getCapability(Capabilities.Fluid.BLOCK, neighborPos, insert) != null
                    || world.getCapability(Capabilities.Energy.BLOCK, neighborPos, insert) != null) {
                return TransferPipeFace.INVENTORY;
            }
        }
        return TransferPipeFace.NONE;
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
        DyeColor dye = DyeColor.getColor(stack);
        if (dye == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        TransferChannel next = TransferChannel.fromDye(dye);
        if (next == channel) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockState recolored = copyConnections(state, ModBlocks.pipe(next).get().defaultBlockState());
        level.setBlock(pos, withConnections(recolored, level, pos), Block.UPDATE_ALL);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
        TransferNetworks.invalidate(level, pos);
        return InteractionResult.CONSUME;
    }

    static BlockState copyConnections(BlockState from, BlockState to) {
        BlockState next = to;
        for (Direction direction : Direction.values()) {
            if (to.getBlock() instanceof TransferPipeBlock) {
                next = next.setValue(FACE_BY_DIRECTION.get(direction), face(from, direction));
            }
        }
        return next;
    }

    static boolean keepsDisabled(TransferPipeFace natural) {
        return natural == TransferPipeFace.PIPE || natural == TransferPipeFace.INVENTORY;
    }

    static Direction armAt(BlockState state, BlockGetter level, BlockPos pos, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            TransferPipeFace face = face(state, direction);
            if (!face.hasArm()) {
                continue;
            }
            boolean shortened = face.shortened() || towardHead(level, pos, direction);
            VoxelShape shape = armShape(direction, shortened);
            if (face == TransferPipeFace.INVENTORY) {
                shape = Shapes.or(shape, nozzleShape(direction));
            }
            if (!contains(shape, local)) {
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

    static boolean contains(VoxelShape shape, Vec3 point) {
        var hit = new boolean[] {false};
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            if (point.x >= x0 - 1.0E-4 && point.x <= x1 + 1.0E-4
                    && point.y >= y0 - 1.0E-4 && point.y <= y1 + 1.0E-4
                    && point.z >= z0 - 1.0E-4 && point.z <= z1 + 1.0E-4) {
                hit[0] = true;
            }
        });
        return hit[0];
    }

    static InteractionResult toggleLink(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction direction,
            Player player
    ) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        Direction opposite = direction.getOpposite();
        TransferPipeFace natural = detect(level, neighborPos, direction, channel(level, pos, state));
        boolean hubLink = TransferNetworks.canToggleLink(state, direction, neighbor);
        boolean nozzleLink = isNozzleToggle(state, direction, natural);
        if (!hubLink && !nozzleLink) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean linked;
        BlockState next;
        if (hubLink) {
            linked = TransferNetworks.linked(state, direction, neighbor);
            next = setHubLink(state, direction, !linked);
            level.setBlock(neighborPos, setHubLink(neighbor, opposite, !linked), Block.UPDATE_ALL);
        } else {
            linked = isInventoryFace(state, direction);
            next = setInventoryLink(state, direction, !linked);
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.playSound(
                null,
                pos,
                linked ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN,
                SoundSource.BLOCKS,
                0.55F,
                linked ? 1.25F : 1.1F
        );
        TransferNetworks.invalidate(level, pos);
        if (hubLink) {
            TransferNetworks.invalidate(level, neighborPos);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean isNozzleToggle(BlockState state, Direction direction, TransferPipeFace natural) {
        if (natural != TransferPipeFace.INVENTORY) {
            return false;
        }
        if (state.getBlock() instanceof TransferPipeBlock) {
            TransferPipeFace face = face(state, direction);
            return face == TransferPipeFace.INVENTORY || face == TransferPipeFace.DISABLED;
        }
        return state.getBlock() instanceof TransferNodeBlock
                && state.getValue(TransferNodeBlock.HAS_PIPE)
                && (TransferNodeBlock.face(state, direction) == TransferNodeFace.INVENTORY
                || TransferNodeBlock.face(state, direction) == TransferNodeFace.DISABLED);
    }

    private static boolean isInventoryFace(BlockState state, Direction direction) {
        if (state.getBlock() instanceof TransferPipeBlock) {
            return face(state, direction) == TransferPipeFace.INVENTORY;
        }
        return state.getBlock() instanceof TransferNodeBlock
                && TransferNodeBlock.face(state, direction) == TransferNodeFace.INVENTORY;
    }

    static BlockState setHubLink(BlockState state, Direction direction, boolean linked) {
        if (state.getBlock() instanceof TransferPipeBlock) {
            return state.setValue(
                    FACE_BY_DIRECTION.get(direction),
                    linked ? TransferPipeFace.PIPE : TransferPipeFace.DISABLED
            );
        }
        if (state.getBlock() instanceof TransferNodeBlock) {
            return state.setValue(
                    TransferNodeBlock.FACE_BY_DIRECTION.get(direction),
                    linked ? TransferNodeFace.PIPE : TransferNodeFace.DISABLED
            );
        }
        return state;
    }

    static BlockState setInventoryLink(BlockState state, Direction direction, boolean linked) {
        if (state.getBlock() instanceof TransferPipeBlock) {
            return state.setValue(
                    FACE_BY_DIRECTION.get(direction),
                    linked ? TransferPipeFace.INVENTORY : TransferPipeFace.DISABLED
            );
        }
        if (state.getBlock() instanceof TransferNodeBlock) {
            return state.setValue(
                    TransferNodeBlock.FACE_BY_DIRECTION.get(direction),
                    linked ? TransferNodeFace.INVENTORY : TransferNodeFace.DISABLED
            );
        }
        return state;
    }
}
