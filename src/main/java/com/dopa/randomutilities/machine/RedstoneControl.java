package com.dopa.randomutilities.machine;

/** Block entities that expose Ignore / Low / High redstone gating. */
public interface RedstoneControl {
    RedstoneMode redstoneMode();

    void setRedstoneMode(RedstoneMode mode);
}
