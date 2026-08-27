package com.dopa.randomutilities.core.machine;

import com.dopa.randomutilities.core.util.ActionCooldownFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Smoke feedback when an Ownable machine cannot run (no owner or action cooldown). */
public final class OwnerRequiredFeedback {
    private OwnerRequiredFeedback() {}

    /**
     * Rising-edge machines (simple breaker/placer). Updates {@code actionCooldown} when blocking.
     *
     * @return true if the pulse should not perform work
     */
    public static boolean blockRisingEdge(
            ServerLevel level,
            BlockPos pos,
            OwnableMachine machine,
            int actionCooldown
    ) {
        if (!machine.hasOwner()) {
            ActionCooldownFeedback.smoke(level, pos);
            return true;
        }
        if (actionCooldown > 0) {
            ActionCooldownFeedback.smoke(level, pos);
            return true;
        }
        return false;
    }

    /**
     * After {@code blockRisingEdge} returned true for no owner, apply cooldown once per smoke burst.
     */
    public static int cooldownAfterNoOwnerBlock(int actionCooldown) {
        if (actionCooldown <= 0) {
            return ActionCooldownFeedback.DEFAULT_COOLDOWN_TICKS;
        }
        return actionCooldown;
    }

    /**
     * Continuous while-powered machines (advanced breaker/placer, fishnet).
     * Decrements {@code ownerFeedbackCooldown} and emits smoke when blocked without owner.
     *
     * @return true if the machine should not run this tick
     */
    public static boolean blockWhilePowered(
            ServerLevel level,
            BlockPos pos,
            OwnableMachine machine,
            boolean wouldRun,
            int ownerFeedbackCooldown
    ) {
        if (!wouldRun || machine.hasOwner()) {
            return false;
        }
        if (ownerFeedbackCooldown > 0) {
            return true;
        }
        ActionCooldownFeedback.smoke(level, pos);
        return true;
    }

    public static int tickOwnerFeedbackCooldown(int ownerFeedbackCooldown) {
        return ownerFeedbackCooldown > 0 ? ownerFeedbackCooldown - 1 : 0;
    }

    public static int cooldownAfterPoweredBlock() {
        return ActionCooldownFeedback.DEFAULT_COOLDOWN_TICKS;
    }
}
