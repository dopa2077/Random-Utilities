package com.dopa.randomutilities.core.machine;

import com.dopa.randomutilities.core.util.WorkingVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;

import java.util.function.IntConsumer;
import java.util.function.Predicate;

/** Shared volume scan and client-sync helpers for advanced breaker/placer block entities. */
public final class AdvancedVolumeMachineSupport {
    public static final int VOLUME_SCAN_BUDGET = 512;
    public static final int EMPTY_VOLUME_BACKOFF = 10;

    private AdvancedVolumeMachineSupport() {}

    public static int emptyVolumeBackoff(BlockEntity be, WorkingVolume volume) {
        int cells = volume.boxCellCount(be.getBlockPos());
        return Math.min(100, Math.max(EMPTY_VOLUME_BACKOFF, cells / 64));
    }

    public static BlockPos pickRandomCell(
            BlockEntity be,
            WorkingVolume volume,
            int volumeScanCursor,
            RandomSource random,
            Predicate<BlockPos> valid
    ) {
        int[] seen = {0};
        BlockPos[] chosen = {null};
        BlockPos origin = be.getBlockPos();
        int boxCells = volume.boxCellCount(origin);
        int budget = Math.min(VOLUME_SCAN_BUDGET, Math.max(1, boxCells));
        volume.forEachCellWindow(origin, volumeScanCursor, budget, cell -> {
            if (!valid.test(cell)) {
                return;
            }
            seen[0]++;
            if (random.nextInt(seen[0]) == 0) {
                chosen[0] = cell.immutable();
            }
        });
        return chosen[0];
    }

    public static int advanceScanCursor(int volumeScanCursor, WorkingVolume volume, BlockPos origin) {
        int boxCells = volume.boxCellCount(origin);
        int budget = Math.min(VOLUME_SCAN_BUDGET, Math.max(1, boxCells));
        if (boxCells > 0) {
            return Math.floorMod(volumeScanCursor + budget, boxCells);
        }
        return volumeScanCursor;
    }

    public static void syncToClient(BlockEntity be) {
        be.setChanged();
        if (be.getLevel() != null && !be.getLevel().isClientSide()) {
            BlockState state = be.getBlockState();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), state, state, 3);
        }
    }

    public static CompoundTag createUpdateTag(WorkingVolume volume, int overlayColor) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MaxRange", volume.maxRange());
        tag.putInt("RangeX", volume.rangeX());
        tag.putInt("RangeY", volume.rangeY());
        tag.putInt("RangeZ", volume.rangeZ());
        tag.putInt("OffsetX", volume.offsetX());
        tag.putInt("OffsetY", volume.offsetY());
        tag.putInt("OffsetZ", volume.offsetZ());
        tag.putInt("OverlayColor", overlayColor & 0xFFFFFF);
        return tag;
    }

    public static void applyClientSync(
            ValueInput input,
            WorkingVolume volume,
            IntConsumer overlayColorSetter,
            int defaultOverlayColor
    ) {
        volume.load(input);
        overlayColorSetter.accept(input.getIntOr("OverlayColor", defaultOverlayColor) & 0xFFFFFF);
    }
}
