package com.dopa.randomutilities.magnet;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.magnet.config.MagnetConfig;
import com.dopa.randomutilities.util.GhostItemFilter;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side item/XP pull for a held or inventoried magnet. */
final class MagnetLogic {
    private static final double CLOSE_RANGE = 1.0;
    private static final Map<UUID, Integer> EMPTY_BACKOFF = new HashMap<>();

    private MagnetLogic() {}

    static void tick(Player player) {
        if (player.level().isClientSide() || !player.isAlive() || player.isSpectator()) {
            return;
        }
        ItemStack magnet = MagnetStorage.findActive(player);
        if (magnet == null) {
            EMPTY_BACKOFF.remove(player.getUUID());
            return;
        }
        MagnetContents contents = MagnetStorage.get(magnet);
        if (contents.pauseOnSneak() && player.isShiftKeyDown()) {
            return;
        }
        int interval = MagnetStorage.tickInterval(contents);
        if (player.tickCount % interval != 0) {
            return;
        }
        int backoff = EMPTY_BACKOFF.getOrDefault(player.getUUID(), 0);
        if (backoff > 0) {
            EMPTY_BACKOFF.put(player.getUUID(), backoff - 1);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        double range = contents.range();
        AABB box = player.getBoundingBox().inflate(range);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, entity -> canPullItem(entity, contents));
        boolean moved = pullItems(level, player, contents, items);
        if (contents.pullXp()) {
            moved |= pullXp(level, player, contents, box);
        }
        if (moved) {
            EMPTY_BACKOFF.remove(player.getUUID());
        } else {
            EMPTY_BACKOFF.put(player.getUUID(), Math.min(4, Math.max(1, interval)));
        }
    }

    static void clear(UUID playerId) {
        EMPTY_BACKOFF.remove(playerId);
    }

    private static boolean pullItems(
            ServerLevel level,
            Player player,
            MagnetContents contents,
            List<ItemEntity> items
    ) {
        int remaining = MagnetStorage.entityBatch(contents);
        boolean mixTypes = MagnetStorage.stackCount(contents) > 0;
        Item lockedType = null;
        boolean movedAny = false;
        Vec3 target = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        double speed = MagnetConfig.pullSpeed() * UpgradeConfig.overclockSpeed(MagnetStorage.overclockCount(contents));

        for (ItemEntity entity : items) {
            if (remaining <= 0) {
                break;
            }
            ItemStack stack = entity.getItem();
            Item type = stack.getItem();
            if (!mixTypes) {
                if (lockedType == null) {
                    lockedType = type;
                } else if (type != lockedType) {
                    continue;
                }
            }
            remaining--;
            movedAny = true;
            if (contents.ignorePickupDelay()) {
                entity.setPickUpDelay(0);
            }
            if (contents.collectMode()) {
                collect(entity, player);
            } else {
                attract(entity, target, speed);
            }
            if (contents.particles()) {
                spawnParticles(level, entity.position());
            }
        }
        return movedAny;
    }

    private static boolean pullXp(ServerLevel level, Player player, MagnetContents contents, AABB box) {
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, box, orb -> orb.isAlive());
        if (orbs.isEmpty()) {
            return false;
        }
        int remaining = MagnetStorage.entityBatch(contents);
        Vec3 target = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        double speed = MagnetConfig.pullSpeed() * UpgradeConfig.overclockSpeed(MagnetStorage.overclockCount(contents));
        boolean moved = false;
        for (ExperienceOrb orb : orbs) {
            if (remaining-- <= 0) {
                break;
            }
            moved = true;
            if (contents.collectMode()) {
                orb.setPos(player.getX(), player.getY(), player.getZ());
                orb.setDeltaMovement(Vec3.ZERO);
            } else {
                attractEntity(orb, target, speed);
            }
            if (contents.particles()) {
                spawnParticles(level, orb.position());
            }
        }
        return moved;
    }

    private static boolean canPullItem(ItemEntity entity, MagnetContents contents) {
        if (!entity.isAlive() || entity.getItem().isEmpty()) {
            return false;
        }
        if (!contents.ignorePickupDelay() && entity.hasPickUpDelay()) {
            return false;
        }
        return GhostItemFilter.allows(entity.getItem(), contents.filterSlots(), contents.whitelist());
    }

    private static void attract(ItemEntity entity, Vec3 target, double speed) {
        attractEntity(entity, target, speed);
        if (entity.position().distanceToSqr(target) <= CLOSE_RANGE * CLOSE_RANGE) {
            entity.setPickUpDelay(0);
        }
    }

    private static void attractEntity(net.minecraft.world.entity.Entity entity, Vec3 target, double speed) {
        Vec3 delta = target.subtract(entity.position());
        double dist = delta.length();
        if (dist < 0.05) {
            return;
        }
        Vec3 motion = delta.scale(speed / dist);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.4).add(motion));
    }

    private static void collect(ItemEntity entity, Player player) {
        entity.setPos(player.getX(), player.getY() + 0.1, player.getZ());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(0);
    }

    private static void spawnParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.01);
    }
}
