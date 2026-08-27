package com.dopa.randomutilities.machine.solar.panel;

import com.dopa.randomutilities.machine.solar.panel.config.SolarPanelConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Connected-blob BFS and formed neighbor bits for a solar controller snapshot. */
public final class SolarArray {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private SolarArray() {}

    public static BlockPos seedPos(BlockPos controllerPos) {
        return controllerPos.above();
    }

    public static boolean isUsableSeed(Level level, BlockPos controllerPos) {
        BlockPos seed = seedPos(controllerPos);
        if (!(level.getBlockState(seed).getBlock() instanceof SolarPanelBlock)) {
            return false;
        }
        return level.getBlockEntity(seed) instanceof SolarPanelBlockEntity seedBe
                && seedBe.isFreeOrOwnedBy(level, controllerPos);
    }

    /**
     * Orthogonal BFS from the panel above the controller, Chebyshev-clamped to
     * {@link SolarPanelConfig#maxRange()} from the controller. Mixes tiers.
     * Returns an empty list if the seed is missing or claimed by someone else.
     */
    public static List<BlockPos> collect(Level level, BlockPos controllerPos) {
        if (!isUsableSeed(level, controllerPos)) {
            return List.of();
        }
        int range = SolarPanelConfig.maxRange();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> blob = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos seed = seedPos(controllerPos).immutable();
        queue.add(seed);
        visited.add(seed);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!(level.getBlockState(current).getBlock() instanceof SolarPanelBlock)) {
                continue;
            }
            if (!(level.getBlockEntity(current) instanceof SolarPanelBlockEntity panelBe)) {
                continue;
            }
            if (!panelBe.isFreeOrOwnedBy(level, controllerPos)) {
                continue;
            }
            blob.add(current);

            for (Direction dir : HORIZONTAL) {
                cursor.setWithOffset(current, dir);
                if (chebyshevHorizontal(controllerPos, cursor) > range) {
                    continue;
                }
                if (!(level.getBlockState(cursor).getBlock() instanceof SolarPanelBlock)) {
                    continue;
                }
                BlockPos next = cursor.immutable();
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return blob;
    }

    public static BlockState formedPanelState(BlockState state, Set<BlockPos> blob, BlockPos pos) {
        return state
                .setValue(SolarPanelBlock.FORMED, true)
                .setValue(SolarPanelBlock.NORTH, blob.contains(pos.north()))
                .setValue(SolarPanelBlock.EAST, blob.contains(pos.east()))
                .setValue(SolarPanelBlock.SOUTH, blob.contains(pos.south()))
                .setValue(SolarPanelBlock.WEST, blob.contains(pos.west()));
    }

    public static BlockState unformedPanelState(BlockState state) {
        if (!state.hasProperty(SolarPanelBlock.FORMED)) {
            return state;
        }
        return state
                .setValue(SolarPanelBlock.FORMED, false)
                .setValue(SolarPanelBlock.NORTH, false)
                .setValue(SolarPanelBlock.EAST, false)
                .setValue(SolarPanelBlock.SOUTH, false)
                .setValue(SolarPanelBlock.WEST, false);
    }

    public static void applyFormed(Level level, Set<BlockPos> blob) {
        for (BlockPos pos : blob) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SolarPanelBlock)) {
                continue;
            }
            BlockState next = formedPanelState(state, blob, pos);
            if (next != state) {
                level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void clearFormed(Level level, Iterable<BlockPos> blob, @Nullable BlockPos skip) {
        for (BlockPos pos : blob) {
            if (skip != null && skip.equals(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SolarPanelBlock)) {
                continue;
            }
            BlockState next = unformedPanelState(state);
            if (next != state) {
                level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void setControllerFormed(Level level, BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SolarPanelControllerBlock)
                || !state.hasProperty(SolarPanelControllerBlock.FORMED)
                || state.getValue(SolarPanelControllerBlock.FORMED) == formed) {
            return;
        }
        level.setBlock(pos, state.setValue(SolarPanelControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
    }

    /** One-shot burst when an array is formed or recast. Not used while generating. */
    public static void spawnFormParticles(ServerLevel level, BlockPos controller, Iterable<BlockPos> panels) {
        for (BlockPos pos : panels) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.32;
            double z = pos.getZ() + 0.5;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 4, 0.22, 0.02, 0.22, 0.01);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 6, 0.28, 0.04, 0.28, 0.0);
        }
        double cx = controller.getX() + 0.5;
        double cy = controller.getY() + 1.05;
        double cz = controller.getZ() + 0.5;
        level.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 6, 0.2, 0.08, 0.2, 0.02);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 8, 0.25, 0.1, 0.25, 0.0);
    }

    private static int chebyshevHorizontal(BlockPos origin, BlockPos cell) {
        return Math.max(Math.abs(cell.getX() - origin.getX()), Math.abs(cell.getZ() - origin.getZ()));
    }
}
