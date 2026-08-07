package com.dopa.randomutilities.redstoneclock;

import com.dopa.randomutilities.machine.RedstoneControl;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RedstoneClockBlockEntity extends BlockEntity implements RedstoneControl {
    public static final int MIN_INTERVAL = 1;
    /** No gameplay cap — only limited by {@code int} range. */
    public static final int MAX_INTERVAL = Integer.MAX_VALUE;
    public static final int DEFAULT_INTERVAL = 20;
    public static final int DEFAULT_PULSE = 2;

    private int interval = DEFAULT_INTERVAL;
    private int pulseLength = DEFAULT_PULSE;
    private int cycleTick;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;

    public RedstoneClockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_CLOCK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RedstoneClockBlockEntity be) {
        be.tickClock(level, pos, state);
    }

    public void onNeighborChanged() {
        if (level == null || level.isClientSide() || RedstoneClockBlock.isUpdating()) {
            return;
        }
        refreshRedstoneGate(level, worldPosition, getBlockState());
    }

    /** Advances the cycle; called only from the block-entity ticker. */
    private void tickClock(Level level, BlockPos pos, BlockState state) {
        boolean allowed = redstoneMode.allowsOperation(RedstoneClockBlock.readControlSignal(level, pos));
        int safeInterval = clampInterval(interval);
        int safePulse = clampPulse(pulseLength, safeInterval);

        boolean powered;
        if (!allowed) {
            cycleTick = 0;
            powered = false;
        } else {
            powered = cycleTick < safePulse;
            cycleTick++;
            if (cycleTick >= safeInterval) {
                cycleTick = 0;
            }
        }

        RedstoneClockBlock.updateRunningPowered(level, pos, state, allowed, powered);
    }

    /** Re-evaluates redstone gating without advancing {@link #cycleTick}. */
    private void refreshRedstoneGate(Level level, BlockPos pos, BlockState state) {
        boolean allowed = redstoneMode.allowsOperation(RedstoneClockBlock.readControlSignal(level, pos));
        int safeInterval = clampInterval(interval);
        int safePulse = clampPulse(pulseLength, safeInterval);

        boolean powered;
        if (!allowed) {
            cycleTick = 0;
            powered = false;
        } else {
            powered = cycleTick < safePulse;
        }

        RedstoneClockBlock.updateRunningPowered(level, pos, state, allowed, powered);
    }

    public Component getDisplayName() {
        return Component.translatable("container.dopasrandomutilities.redstone_clock");
    }

    public int interval() {
        return interval;
    }

    public int pulseLength() {
        return pulseLength;
    }

    public void setInterval(int value) {
        interval = clampInterval(value);
        pulseLength = clampPulse(pulseLength, interval);
        cycleTick = Math.min(cycleTick, Math.max(0, interval - 1));
        setChanged();
    }

    public void setPulseLength(int value) {
        pulseLength = clampPulse(value, clampInterval(interval));
        setChanged();
    }

    @Override
    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    @Override
    public void setRedstoneMode(RedstoneMode mode) {
        this.redstoneMode = mode == null ? RedstoneMode.IGNORE : mode;
        setChanged();
    }

    public static int clampInterval(int value) {
        return clampInterval((long) value);
    }

    public static int clampInterval(long value) {
        if (value < MIN_INTERVAL) {
            return MIN_INTERVAL;
        }
        if (value > MAX_INTERVAL) {
            return MAX_INTERVAL;
        }
        return (int) value;
    }

    public static int clampPulse(int value, int interval) {
        return clampPulse((long) value, interval);
    }

    public static int clampPulse(long value, int interval) {
        int max = Math.max(MIN_INTERVAL, interval);
        if (value < MIN_INTERVAL) {
            return MIN_INTERVAL;
        }
        if (value > max) {
            return max;
        }
        return (int) value;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        interval = clampInterval(input.getIntOr("Interval", DEFAULT_INTERVAL));
        pulseLength = clampPulse(input.getIntOr("PulseLength", DEFAULT_PULSE), interval);
        cycleTick = Mth.clamp(input.getIntOr("CycleTick", 0), 0, Math.max(0, interval - 1));
        redstoneMode = RedstoneMode.byOrdinal(input.getIntOr("RedstoneMode", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Interval", interval);
        output.putInt("PulseLength", pulseLength);
        output.putInt("CycleTick", cycleTick);
        output.putInt("RedstoneMode", redstoneMode.ordinal());
    }
}
