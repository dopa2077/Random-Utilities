package com.dopa.randomutilities.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/**
 * Axis-aligned working volume for advanced breaker/placer.
 * Box is the block at {@code machine + offset}, inflated by range, then clipped to chebyshev
 * {@link #maxRange()} from the machine. Range upgrades raise the range/chebyshev cap above
 * {@link #MAX_RANGE}; offset is capped separately by {@link #MAX_OFFSET}.
 */
public final class WorkingVolume {
    public static final int MAX_RANGE = 5;
    public static final int MAX_OFFSET = 10;

    public static final byte KIND_RANGE_X = 0;
    public static final byte KIND_RANGE_Y = 1;
    public static final byte KIND_RANGE_Z = 2;
    public static final byte KIND_OFFSET_X = 3;
    public static final byte KIND_OFFSET_Y = 4;
    public static final byte KIND_OFFSET_Z = 5;
    public static final byte KIND_MUTE = 6;
    public static final byte KIND_FILTER_MODE = 7;
    public static final byte KIND_REDSTONE = 8;
    public static final byte KIND_COLOR = 9;

    private int rangeX;
    private int rangeY;
    private int rangeZ;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private int maxRange = MAX_RANGE;
    private boolean boundsDirty = true;
    private int boundsMachineX = Integer.MIN_VALUE;
    private int boundsMachineY;
    private int boundsMachineZ;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public void setOffsetToFacing(Direction facing) {
        offsetX = facing.getStepX();
        offsetY = facing.getStepY();
        offsetZ = facing.getStepZ();
        boundsDirty = true;
    }

    public int rangeX() {
        return rangeX;
    }

    public int rangeY() {
        return rangeY;
    }

    public int rangeZ() {
        return rangeZ;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public int offsetZ() {
        return offsetZ;
    }

    public int maxRange() {
        return maxRange;
    }

    public void setMaxRange(int maxRange) {
        this.maxRange = Math.max(MAX_RANGE, maxRange);
        rangeX = clampRange(rangeX);
        rangeY = clampRange(rangeY);
        rangeZ = clampRange(rangeZ);
        offsetX = clampOffset(offsetX);
        offsetY = clampOffset(offsetY);
        offsetZ = clampOffset(offsetZ);
        boundsDirty = true;
    }

    public void setRangeX(int value) {
        rangeX = clampRange(value);
        boundsDirty = true;
    }

    public void setRangeY(int value) {
        rangeY = clampRange(value);
        boundsDirty = true;
    }

    public void setRangeZ(int value) {
        rangeZ = clampRange(value);
        boundsDirty = true;
    }

    public void setOffsetX(int value) {
        offsetX = clampOffset(value);
        boundsDirty = true;
    }

    public void setOffsetY(int value) {
        offsetY = clampOffset(value);
        boundsDirty = true;
    }

    public void setOffsetZ(int value) {
        offsetZ = clampOffset(value);
        boundsDirty = true;
    }

    public int clampRange(int value) {
        return Mth.clamp(value, 0, maxRange);
    }

    public static int clampRange(int value, int maxRange) {
        return Mth.clamp(value, 0, Math.max(0, maxRange));
    }

    public int clampOffset(int value) {
        return clampOffset(value, MAX_OFFSET);
    }

    public static int clampOffset(int value, int maxOffset) {
        int cap = Math.max(0, maxOffset);
        return Mth.clamp(value, -cap, cap);
    }

    /** Inclusive AABB of the unclipped inflate box (block at offset, grown by range). */
    public AABB box(BlockPos machine) {
        BlockPos center = machine.offset(offsetX, offsetY, offsetZ);
        return new AABB(center).inflate(rangeX, rangeY, rangeZ);
    }

    /** Inflate box clipped to chebyshev {@link #maxRange()} from the machine. */
    public AABB overlayBox(BlockPos machine) {
        AABB volume = box(machine);
        AABB limit = new AABB(machine).inflate(maxRange);
        if (!volume.intersects(limit)) {
            return volume;
        }
        return volume.intersect(limit);
    }

    public boolean contains(BlockPos machine, BlockPos cell) {
        ensureBounds(machine);
        if (cell.getX() < minX || cell.getX() > maxX
                || cell.getY() < minY || cell.getY() > maxY
                || cell.getZ() < minZ || cell.getZ() > maxZ) {
            return false;
        }
        return chebyshev(machine, cell) <= maxRange;
    }

    public void forEachCell(BlockPos machine, Consumer<BlockPos> consumer) {
        ensureBounds(machine);
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (chebyshev(machine, cursor) <= maxRange) {
                        consumer.accept(cursor);
                    }
                }
            }
        }
    }

    /** Inclusive AABB cell count before chebyshev clip (upper bound on work). */
    public int boxCellCount(BlockPos machine) {
        ensureBounds(machine);
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return 0;
        }
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /**
     * Visits up to {@code budget} box cells starting at {@code startIndex} (wrapping),
     * skipping those outside the chebyshev range. Avoids full-volume scans each tick.
     */
    public void forEachCellWindow(BlockPos machine, int startIndex, int budget, Consumer<BlockPos> consumer) {
        ensureBounds(machine);
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || budget <= 0) {
            return;
        }
        long total = (long) sizeX * sizeY * sizeZ;
        int toVisit = (int) Math.min(budget, total);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < toVisit; i++) {
            long idx = Math.floorMod((long) startIndex + i, total);
            int z = (int) (idx % sizeZ);
            long t = idx / sizeZ;
            int y = (int) (t % sizeY);
            int x = (int) (t / sizeY);
            cursor.set(minX + x, minY + y, minZ + z);
            if (chebyshev(machine, cursor) <= maxRange) {
                consumer.accept(cursor);
            }
        }
    }

    private void ensureBounds(BlockPos machine) {
        if (!boundsDirty
                && boundsMachineX == machine.getX()
                && boundsMachineY == machine.getY()
                && boundsMachineZ == machine.getZ()) {
            return;
        }
        AABB box = overlayBox(machine);
        minX = Mth.floor(box.minX);
        minY = Mth.floor(box.minY);
        minZ = Mth.floor(box.minZ);
        maxX = Mth.ceil(box.maxX) - 1;
        maxY = Mth.ceil(box.maxY) - 1;
        maxZ = Mth.ceil(box.maxZ) - 1;
        boundsMachineX = machine.getX();
        boundsMachineY = machine.getY();
        boundsMachineZ = machine.getZ();
        boundsDirty = false;
    }

    public static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(
                Math.abs(a.getX() - b.getX()),
                Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ()))
        );
    }

    public void load(ValueInput input) {
        maxRange = Math.max(MAX_RANGE, input.getIntOr("MaxRange", maxRange));
        rangeX = clampRange(input.getIntOr("RangeX", rangeX));
        rangeY = clampRange(input.getIntOr("RangeY", rangeY));
        rangeZ = clampRange(input.getIntOr("RangeZ", rangeZ));
        offsetX = clampOffset(input.getIntOr("OffsetX", offsetX));
        offsetY = clampOffset(input.getIntOr("OffsetY", offsetY));
        offsetZ = clampOffset(input.getIntOr("OffsetZ", offsetZ));
        boundsDirty = true;
    }

    public void save(ValueOutput output) {
        output.putInt("RangeX", rangeX);
        output.putInt("RangeY", rangeY);
        output.putInt("RangeZ", rangeZ);
        output.putInt("OffsetX", offsetX);
        output.putInt("OffsetY", offsetY);
        output.putInt("OffsetZ", offsetZ);
    }
}
