package com.dopa.randomutilities.lasso;

import com.dopa.randomutilities.machine.ClaimActionGate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class LassoSpawnerRewrite {
    private LassoSpawnerRewrite() {}

    public enum Failure {
        EMPTY,
        NOT_SPAWNER,
        INCOMPATIBLE,
        CLAIM_DENIED,
        ALREADY_SET
    }

    public static Optional<Component> tryRewrite(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            ItemStack stack
    ) {
        LassoCapture capture = LassoCapture.get(stack);
        if (capture == null) {
            return fail(level, player, Failure.EMPTY);
        }
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) {
            return fail(level, player, Failure.NOT_SPAWNER);
        }
        EntityType<?> entityType = capture.entityType();
        if (entityType == null || !entityType.isEnabled(level.enabledFeatures())) {
            return fail(level, player, Failure.INCOMPATIBLE);
        }
        if (!ClaimActionGate.canBreak(level, player, pos)) {
            return fail(level, player, Failure.CLAIM_DENIED);
        }
        Entity display = spawner.getSpawner().getOrCreateDisplayEntity(level, pos);
        if (display != null && display.getType() == entityType) {
            return fail(level, player, Failure.ALREADY_SET);
        }

        spawner.setEntityId(entityType, level.getRandom());
        spawner.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        LassoFeedback.playSpawnerRewriteSuccess(level, pos);
        stack.shrink(1);
        return Optional.empty();
    }

    private static Optional<Component> fail(ServerLevel level, ServerPlayer player, Failure failure) {
        LassoFeedback.playDenial(level, player);
        return LassoLogic.translatedFailure("item.dopasrandomutilities.cursed_lasso.spawner.", failure);
    }
}
