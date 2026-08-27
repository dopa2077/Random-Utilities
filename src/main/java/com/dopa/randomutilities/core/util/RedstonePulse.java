package com.dopa.randomutilities.core.util;

import com.dopa.randomutilities.core.machine.RedstoneMode;

/**
 * Edge detector for redstone-triggered machines.
 * One pulse per unpowered → powered transition (or the reverse for {@link RedstoneMode#LOW}).
 */
public final class RedstonePulse {
    private boolean wasPowered;

    public boolean risingEdge(boolean powered) {
        boolean pulse = powered && !wasPowered;
        wasPowered = powered;
        return pulse;
    }

    /**
     * IGNORE / HIGH fire on a rising edge; LOW fires on a falling edge.
     * Never free-runs while a signal is held. Call once per tick — {@link #wasPowered} is updated here.
     */
    public boolean shouldFire(RedstoneMode mode, boolean powered) {
        boolean pulse = switch (mode == null ? RedstoneMode.IGNORE : mode) {
            case LOW -> !powered && wasPowered;
            case IGNORE, HIGH -> powered && !wasPowered;
        };
        wasPowered = powered;
        return pulse;
    }

    public boolean wasPowered() {
        return wasPowered;
    }

    public void setWasPowered(boolean wasPowered) {
        this.wasPowered = wasPowered;
    }
}
