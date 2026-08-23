package com.dopa.randomutilities.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/** Soft feedback when a redstone-pulsed machine is still cooling down. */
public final class ActionCooldownFeedback {
    public static final int DEFAULT_COOLDOWN_TICKS = 20;

    private ActionCooldownFeedback() {}

    public static void smoke(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                3,
                0.12,
                0.05,
                0.12,
                0.01
        );
    }
}
