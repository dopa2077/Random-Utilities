package com.dopa.randomutilities.machine;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Block entities that record the placing player for claim-aware FakePlayer actions. */
public interface OwnableMachine {
    @Nullable
    UUID ownerUuid();

    void setOwnerUuid(@Nullable UUID uuid);

    default boolean hasOwner() {
        return ownerUuid() != null;
    }

    static void bindPlacer(BlockEntity be, @Nullable LivingEntity placer) {
        if (be instanceof OwnableMachine ownable && placer instanceof Player player) {
            ownable.setOwnerUuid(player.getUUID());
        }
    }
}
