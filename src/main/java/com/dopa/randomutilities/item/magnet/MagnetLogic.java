package com.dopa.randomutilities.item.magnet;

import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.item.magnet.config.MagnetConfig;
import com.dopa.randomutilities.core.util.GhostItemFilter;

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
    private static final double CONTINUOUS_DAMP = 0.7;
    private static final double ATTRACT_SPEED_SCALE = 0.45;
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
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int interval = MagnetStorage.tickInterval(contents);
        boolean pulse = player.tickCount % interval == 0;
        int backoff = EMPTY_BACKOFF.getOrDefault(player.getUUID(), 0);
        if (backoff > 0) {
            EMPTY_BACKOFF.put(player.getUUID(), backoff - 1);
            if (contents.collectMode()) {
                return;
            }
        }

        double range = contents.range();
        AABB box = player.getBoundingBox().inflate(range);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, entity -> canPullItem(entity, contents));
        boolean moved;
        if (contents.collectMode()) {
            if (!pulse) {
                return;
            }
            moved = pullItems(level, player, contents, items, true);
        } else {
            // Attract every tick so high intervals stay smooth; budget/type lock still apply.
            moved = pullItems(level, player, contents, items, false);
        }
        if (contents.pullXp()) {
            moved |= pullXp(level, player, contents, box, contents.collectMode() ? pulse : true);
        }
        if (contents.collectMode()) {
            if (moved) {
                EMPTY_BACKOFF.remove(player.getUUID());
            } else if (pulse) {
                EMPTY_BACKOFF.put(player.getUUID(), Math.min(4, Math.max(1, interval)));
            }
        } else if (moved) {
            EMPTY_BACKOFF.remove(player.getUUID());
        }
    }

    static void clear(UUID playerId) {
        EMPTY_BACKOFF.remove(playerId);
    }

    private static boolean pullItems(
            ServerLevel level,
            Player player,
            MagnetContents contents,
            List<ItemEntity> items,
            boolean collect
    ) {
        int remaining = MagnetStorage.pickupBudget(contents);
        boolean mixTypes = UpgradeConfig.stackAllowsMixedTypes(MagnetStorage.stackCount(contents));
        Item lockedType = null;
        boolean movedAny = false;
        Vec3 target = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        double speed = MagnetConfig.pullSpeed()
                * UpgradeConfig.overclockSpeed(MagnetStorage.overclockCount(contents))
                * ATTRACT_SPEED_SCALE;
        for (ItemEntity entity : items) {
            if (remaining <= 0) {
                break;
            }
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            Item type = stack.getItem();
            if (!mixTypes) {
                if (lockedType == null) {
                    lockedType = type;
                } else if (type != lockedType) {
                    continue;
                }
            }
            // Budget is item-count based; still pull the entity even if its stack is larger.
            remaining -= Math.min(stack.getCount(), remaining);
            movedAny = true;
            if (contents.ignorePickupDelay()) {
                entity.setPickUpDelay(0);
            }
            if (collect) {
                collect(entity, player);
            } else {
                attract(entity, target, speed);
            }
            if (contents.particles()) {
                spawnTrail(level, entity.position(), target);
            }
        }
        return movedAny;
    }

    private static boolean pullXp(
            ServerLevel level,
            Player player,
            MagnetContents contents,
            AABB box,
            boolean allow
    ) {
        if (!allow) {
            return false;
        }
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, box, orb -> orb.isAlive());
        if (orbs.isEmpty()) {
            return false;
        }
        int remaining = MagnetStorage.xpOrbBudget(contents);
        Vec3 target = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        double speed = MagnetConfig.pullSpeed()
                * UpgradeConfig.overclockSpeed(MagnetStorage.overclockCount(contents))
                * ATTRACT_SPEED_SCALE;
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
                spawnTrail(level, orb.position(), target);
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
        // Continuous per-tick pull; damp previous motion so long intervals still look smooth.
        Vec3 motion = delta.normalize().scale(speed);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(CONTINUOUS_DAMP).add(motion));
    }

    private static void collect(ItemEntity entity, Player player) {
        entity.setPos(player.getX(), player.getY() + 0.1, player.getZ());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(0);
    }

    private static void spawnTrail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double dist = delta.length();
        if (dist < 0.05) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 1, 0.02, 0.02, 0.02, 0.0);
            return;
        }
        // Sparse trail: a few points along the path, not every tick-dense step.
        int steps = Math.max(2, Math.min(5, (int) Math.ceil(dist * 0.75)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 point = from.add(delta.scale(t));
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.01,
                    0.01,
                    0.01,
                    0.0
            );
        }
    }
}
