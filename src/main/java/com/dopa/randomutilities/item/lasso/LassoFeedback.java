package com.dopa.randomutilities.item.lasso;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Client-visible capture, deploy, and denial feedback for lassos. */
final class LassoFeedback {
    private static final int POOF_COUNT = 14;
    private static final int PORTAL_COUNT = 8;
    private static final double POOF_SPREAD = 0.28;
    private static final double POOF_HEIGHT = 0.4;
    private static final double PORTAL_SPREAD = 0.02;

    private LassoFeedback() {}

    static void playCaptureSuccess(ServerLevel level, BlockPos blockPos, Vec3 feet) {
        playCapturePoof(level, feet);
        playPortalBurst(level, blockPos);
        playPickupSound(level, feet);
    }

    static void playDeploySuccess(ServerLevel level, BlockPos blockPos, Vec3 feet) {
        playPortalBurst(level, blockPos);
        playPickupSound(level, feet);
    }

    static void playSpawnerRewriteSuccess(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.65;
        double z = pos.getZ() + 0.5;
        level.playSound(null, pos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 0.9F, 0.75F);
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 14, 0.35, 0.25, 0.35, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 8, 0.2, 0.15, 0.2, 0.01);
    }

    static void playDenial(ServerLevel level, ServerPlayer player) {
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.NOTE_BLOCK_BASS,
                SoundSource.PLAYERS,
                0.75F,
                0.65F
        );
    }

    private static void playCapturePoof(ServerLevel level, Vec3 feet) {
        level.sendParticles(
                ParticleTypes.POOF,
                feet.x,
                feet.y + POOF_HEIGHT,
                feet.z,
                POOF_COUNT,
                POOF_SPREAD,
                0.08,
                POOF_SPREAD,
                0.02
        );
    }

    private static void playPortalBurst(ServerLevel level, BlockPos blockPos) {
        Vec3 pos = Vec3.atBottomCenterOf(blockPos).add(0, 0.08, 0);
        level.sendParticles(
                ParticleTypes.PORTAL,
                pos.x,
                pos.y,
                pos.z,
                PORTAL_COUNT,
                PORTAL_SPREAD,
                0.01,
                PORTAL_SPREAD,
                0.0
        );
    }

    private static void playPickupSound(ServerLevel level, Vec3 at) {
        level.playSound(
                null,
                at.x,
                at.y,
                at.z,
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.35F,
                1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F
        );
    }
}
