package com.dopa.randomutilities.transfer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class TransferNetworks {
    public static final int MAX_PIPES = 256;
    public static final int BACKOFF_TICKS = 8;

    private static final Map<ServerLevel, Set<BlockPos>> PENDING = new IdentityHashMap<>();

    private TransferNetworks() {}

    public record Destination(BlockPos inventoryPos, Direction insertFace) {}

    public record Island(
            List<Destination> items,
            List<Destination> fluids,
            List<Destination> energy
    ) {
        static Island empty() {
            return new Island(List.of(), List.of(), List.of());
        }
    }

    public static boolean isNetworkBlock(BlockState state) {
        return state.getBlock() instanceof TransferPipeBlock
                || state.getBlock() instanceof TransferNodeBlock;
    }

    public static boolean isPipeHub(BlockState state) {
        // Any transfer node participates in island membership (heads need destination lists).
        // Inventory nozzles are still only collected from faces marked INVENTORY.
        return state.getBlock() instanceof TransferPipeBlock
                || state.getBlock() instanceof TransferNodeBlock;
    }

    public static boolean linksToward(BlockState state, Direction direction) {
        if (state.getBlock() instanceof TransferPipeBlock) {
            return TransferPipeBlock.face(state, direction) == TransferPipeFace.PIPE;
        }
        return state.getBlock() instanceof TransferNodeBlock
                && state.getValue(TransferNodeBlock.HAS_PIPE)
                && TransferNodeBlock.face(state, direction) == TransferNodeFace.PIPE;
    }

    /**
     * Mutual pipe/node PIPE links, plus pipe arms into a headed node face
     * (heads force that node face to {@link TransferNodeFace#NONE} for visuals).
     */
    public static boolean networkConnects(
            Level level,
            BlockPos fromPos,
            BlockState from,
            Direction direction,
            BlockPos toPos,
            BlockState to
    ) {
        if (linksToward(from, direction) && linksToward(to, direction.getOpposite())) {
            return true;
        }
        return pipeIntoHead(level, fromPos, from, direction, toPos, to)
                || pipeIntoHead(level, toPos, to, direction.getOpposite(), fromPos, from);
    }

    private static boolean pipeIntoHead(
            Level level,
            BlockPos pipePos,
            BlockState pipeState,
            Direction towardNode,
            BlockPos nodePos,
            BlockState nodeState
    ) {
        if (!(pipeState.getBlock() instanceof TransferPipeBlock)) {
            return false;
        }
        if (TransferPipeBlock.face(pipeState, towardNode) != TransferPipeFace.PIPE) {
            return false;
        }
        if (!(nodeState.getBlock() instanceof TransferNodeBlock)) {
            return false;
        }
        return TransferNodeBlock.hasHead(level, nodePos, towardNode.getOpposite());
    }

    public static boolean linked(BlockState from, Direction direction, BlockState to) {
        return linksToward(from, direction) && linksToward(to, direction.getOpposite());
    }

    public static boolean canToggleLink(BlockState from, Direction direction, BlockState to) {
        return isToggleFace(from, direction) && isToggleFace(to, direction.getOpposite());
    }

    private static boolean isToggleFace(BlockState state, Direction direction) {
        if (state.getBlock() instanceof TransferPipeBlock) {
            TransferPipeFace face = TransferPipeBlock.face(state, direction);
            return face == TransferPipeFace.PIPE || face == TransferPipeFace.DISABLED;
        }
        if (state.getBlock() instanceof TransferNodeBlock && state.getValue(TransferNodeBlock.HAS_PIPE)) {
            TransferNodeFace face = TransferNodeBlock.face(state, direction);
            return face == TransferNodeFace.PIPE || face == TransferNodeFace.DISABLED;
        }
        return false;
    }

    public static boolean hasInventoryConnection(BlockState state, Direction direction) {
        if (state.getBlock() instanceof TransferPipeBlock) {
            return TransferPipeBlock.face(state, direction) == TransferPipeFace.INVENTORY;
        }
        return state.getBlock() instanceof TransferNodeBlock
                && TransferNodeBlock.face(state, direction) == TransferNodeFace.INVENTORY;
    }

    public static boolean topologyChanged(BlockState oldState, BlockState newState) {
        if (oldState.getBlock() != newState.getBlock()) {
            return isNetworkBlock(oldState) || isNetworkBlock(newState);
        }
        if (newState.getBlock() instanceof TransferPipeBlock) {
            for (Direction direction : Direction.values()) {
                if (TransferPipeBlock.face(oldState, direction) != TransferPipeBlock.face(newState, direction)) {
                    return true;
                }
            }
            return false;
        }
        if (newState.getBlock() instanceof TransferNodeBlock) {
            if (oldState.getValue(TransferNodeBlock.HAS_PIPE) != newState.getValue(TransferNodeBlock.HAS_PIPE)) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                if (TransferNodeBlock.face(oldState, direction) != TransferNodeBlock.face(newState, direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void invalidate(Level level, BlockPos origin) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        PENDING.computeIfAbsent(server, unused -> new HashSet<>()).add(origin.immutable());
    }

    public static void drop(ServerLevel level) {
        PENDING.remove(level);
    }

    public static void flush(ServerLevel level) {
        Set<BlockPos> pending = PENDING.remove(level);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        Set<BlockPos> covered = new HashSet<>();
        for (BlockPos origin : pending) {
            if (covered.contains(origin)) {
                continue;
            }
            rebuildIsland(level, origin, covered);
        }
    }

    public static void rebuildNow(ServerLevel level, BlockPos origin) {
        rebuildIsland(level, origin, new HashSet<>());
    }

    public static void clear() {
        PENDING.clear();
    }

    private static void rebuildIsland(ServerLevel level, BlockPos origin, Set<BlockPos> covered) {
        if (!isNetworkBlock(level.getBlockState(origin))) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = origin.relative(direction).immutable();
                if (covered.contains(neighbor) || !isNetworkBlock(level.getBlockState(neighbor))) {
                    continue;
                }
                rebuildIsland(level, neighbor, covered);
            }
            return;
        }
        Set<BlockPos> members = floodFill(level, origin);
        covered.addAll(members);
        Island island = collectIsland(level, members);
        for (BlockPos pos : members) {
            if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
                be.receiveIsland(island);
            }
        }
    }

    static Island collectIsland(ServerLevel level, Set<BlockPos> members) {
        return new Island(
                collectDestinations(level, members, Capabilities.Item.BLOCK),
                collectDestinations(level, members, Capabilities.Fluid.BLOCK),
                collectDestinations(level, members, Capabilities.Energy.BLOCK)
        );
    }

    static List<Destination> collectInventories(ServerLevel level, Set<BlockPos> members) {
        return collectDestinations(level, members, Capabilities.Item.BLOCK);
    }

    private static List<Destination> collectDestinations(
            ServerLevel level,
            Set<BlockPos> members,
            net.neoforged.neoforge.capabilities.BlockCapability<?, Direction> capability
    ) {
        List<Destination> destinations = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (BlockPos pos : members) {
            BlockState state = level.getBlockState(pos);
            // Nozzles live on pipes and pipe-bodied nodes only.
            if (!(state.getBlock() instanceof TransferPipeBlock)
                    && !(state.getBlock() instanceof TransferNodeBlock
                    && state.getValue(TransferNodeBlock.HAS_PIPE))) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (!hasInventoryConnection(state, direction)) {
                    continue;
                }
                BlockPos neighbor = pos.relative(direction);
                if (members.contains(neighbor) || isNetworkBlock(level.getBlockState(neighbor))) {
                    continue;
                }
                Direction insertFace = direction.getOpposite();
                if (level.getCapability(capability, neighbor, insertFace) == null) {
                    continue;
                }
                long key = neighbor.asLong() << 3 | (long) insertFace.ordinal();
                if (seen.add(key)) {
                    destinations.add(new Destination(neighbor.immutable(), insertFace));
                }
            }
        }
        return List.copyOf(destinations);
    }

    private static Set<BlockPos> floodFill(ServerLevel level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        BlockState originState = level.getBlockState(origin);
        if (isNetworkBlock(originState)) {
            BlockPos start = origin.immutable();
            visited.add(start);
            queue.add(start);
        } else {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = origin.relative(direction).immutable();
                if (isNetworkBlock(level.getBlockState(neighbor)) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        int pipes = 0;
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            if (!isPipeHub(state)) {
                continue;
            }
            boolean pipe = state.getBlock() instanceof TransferPipeBlock;
            if (pipe) {
                pipes++;
            }
            if (pipe && pipes > MAX_PIPES) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction).immutable();
                BlockState neighborState = level.getBlockState(neighbor);
                if (!networkConnects(level, current, state, direction, neighbor, neighborState)) {
                    continue;
                }
                TransferChannel from = TransferPipeBlock.channel(level, current, state);
                TransferChannel to = TransferPipeBlock.channel(level, neighbor, neighborState);
                if (!from.connectsTo(to) && TransferNetworks.isPipeHub(neighborState) && TransferNetworks.isPipeHub(state)) {
                    continue;
                }
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }
}
