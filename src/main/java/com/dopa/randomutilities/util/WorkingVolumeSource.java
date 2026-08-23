package com.dopa.randomutilities.util;

/** Block entities that expose a {@link WorkingVolume} for pulse targeting and the world overlay. */
public interface WorkingVolumeSource {
    WorkingVolume workingVolume();

    int overlayColor();
}
