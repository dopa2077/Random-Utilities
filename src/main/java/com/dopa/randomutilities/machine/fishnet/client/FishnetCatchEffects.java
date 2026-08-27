package com.dopa.randomutilities.machine.fishnet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-only swim-into-net VFX. The fish is never added to the level — it is
 * only ticked here and submitted by {@link FishnetRenderer}.
 */
public final class FishnetCatchEffects {
    private static final Map<BlockPos, ActiveCatch> ACTIVE = new HashMap<>();
    /** Dummy ids for display-only fish; 0 is the unassigned sentinel in Entity#getId. */
    private static int nextVisualId = -1;

    private FishnetCatchEffects() {}

    private static int nextVisualId() {
        int id = nextVisualId;
        nextVisualId--;
        if (nextVisualId == 0) {
            nextVisualId = -1;
        }
        return id;
    }

    public static void play(BlockPos netPos, int durationTicks) {
        BlockPos key = netPos.immutable();
        if (durationTicks <= 0) {
            ACTIVE.remove(key);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }

        Vec3 target = Vec3.atCenterOf(netPos);
        var random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 1.6 + random.nextDouble() * 1.2;
        Vec3 start = target.add(
                Math.cos(angle) * distance,
                -0.25 + random.nextDouble() * 0.5,
                Math.sin(angle) * distance
        );

        AbstractFish fish = createFish(level);
        // Never added to the level, but LivingEntityRenderer → ItemModelResolver
        // calls Entity#getId(), which throws if the id is still 0.
        fish.setId(nextVisualId());
        fish.setPos(start.x, start.y, start.z);
        fish.setNoGravity(true);
        fish.setSilent(true);
        fish.setInvulnerable(true);
        fish.setDeltaMovement(Vec3.ZERO);
        fish.setYRot((float) (Math.toDegrees(angle) + 180.0F));
        ACTIVE.put(key, new ActiveCatch(fish, start, target, durationTicks));
    }

    @Nullable
    public static ActiveCatch activeAt(BlockPos netPos) {
        return ACTIVE.get(netPos);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void clientTick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Iterator<Map.Entry<BlockPos, ActiveCatch>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveCatch active = iterator.next().getValue();
            AbstractFish fish = active.fish;
            if (fish.level() == null || level == null || fish.level() != level) {
                iterator.remove();
                continue;
            }
            active.age++;
            if (active.age >= active.duration) {
                iterator.remove();
                continue;
            }
            Vec3 pos = active.position(0.0F);
            Vec3 delta = active.target.subtract(pos);
            double distance = delta.length();
            if (distance < 0.35) {
                iterator.remove();
                continue;
            }
            fish.setPos(pos.x, pos.y, pos.z);
            Vec3 step = delta.normalize().scale(Math.min(0.22, distance));
            fish.setDeltaMovement(step);
            fish.setYRot((float) (Math.toDegrees(Math.atan2(step.z, step.x)) - 90.0F));
            fish.yBodyRot = fish.getYRot();
            fish.yHeadRot = fish.getYRot();
            fish.tickCount++;
        }
    }

    private static AbstractFish createFish(Level level) {
        return switch (level.getRandom().nextInt(4)) {
            case 1 -> new Salmon(EntityTypes.SALMON, level);
            case 2 -> new Pufferfish(EntityTypes.PUFFERFISH, level);
            case 3 -> new TropicalFish(EntityTypes.TROPICAL_FISH, level);
            default -> new Cod(EntityTypes.COD, level);
        };
    }

    public static final class ActiveCatch {
        private final AbstractFish fish;
        private final Vec3 start;
        private final Vec3 target;
        private final int duration;
        private int age;

        private ActiveCatch(AbstractFish fish, Vec3 start, Vec3 target, int duration) {
            this.fish = fish;
            this.start = start;
            this.target = target;
            this.duration = Math.max(1, duration);
        }

        public AbstractFish fish() {
            return fish;
        }

        public Vec3 position(float partialTick) {
            float t = Math.min(1.0F, (age + partialTick) / (float) duration);
            t = t * t * (3.0F - 2.0F * t);
            return start.lerp(target, t);
        }
    }
}
