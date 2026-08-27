package com.dopa.randomutilities.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class MachineActors {
    private MachineActors() {}

    public static Optional<FakePlayer> actor(
            ServerLevel level,
            @Nullable UUID ownerUuid,
            BlockPos machinePos,
            Direction facing
    ) {
        if (ownerUuid == null) {
            return Optional.empty();
        }
        FakePlayer player = FakePlayerFactory.get(level, MachineOwnerProfiles.profileFor(level, ownerUuid));
        positionAt(player, machinePos, facing);
        return Optional.of(player);
    }

    public static void positionAt(Player player, BlockPos pos, Direction facing) {
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(facing.toYRot());
        player.setXRot(pitchFor(facing));
    }

    private static float pitchFor(Direction facing) {
        return switch (facing) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }
}
