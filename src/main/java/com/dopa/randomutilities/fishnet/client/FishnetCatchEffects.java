package com.dopa.randomutilities.fishnet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Client-only swim-into-net effect for a successful fishnet catch. */
public final class FishnetCatchEffects {
    private static final int LIFETIME_TICKS = 20;
    private static final List<ActiveCatch> ACTIVE = new ArrayList<>();

    private FishnetCatchEffects() {}

    public static void play(BlockPos netPos, ItemStack display) {
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

        AbstractFish fish = createFish(level, display);
        fish.setPos(start.x, start.y, start.z);
        fish.setNoGravity(true);
        fish.setSilent(true);
        fish.setInvulnerable(true);
        fish.setDeltaMovement(Vec3.ZERO);
        fish.setYRot((float) (Math.toDegrees(angle) + 180.0F));
        level.addFreshEntity(fish);
        ACTIVE.add(new ActiveCatch(fish, target, LIFETIME_TICKS));
    }

    public static void clientTick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<ActiveCatch> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            ActiveCatch active = iterator.next();
            AbstractFish fish = active.fish;
            if (!fish.isAlive() || fish.level() == null) {
                iterator.remove();
                continue;
            }
            active.life--;
            Vec3 pos = fish.position();
            Vec3 delta = active.target.subtract(pos);
            double distance = delta.length();
            if (active.life <= 0 || distance < 0.35) {
                fish.discard();
                iterator.remove();
                continue;
            }
            Vec3 step = delta.normalize().scale(Math.min(0.22, distance));
            fish.setPos(pos.x + step.x, pos.y + step.y, pos.z + step.z);
            fish.setDeltaMovement(step);
            fish.setYRot((float) (Math.toDegrees(Math.atan2(step.z, step.x)) - 90.0F));
            fish.yBodyRot = fish.getYRot();
            fish.yHeadRot = fish.getYRot();
        }
    }

    private static AbstractFish createFish(Level level, ItemStack display) {
        if (display.is(Items.SALMON)) {
            return new Salmon(EntityTypes.SALMON, level);
        }
        if (display.is(Items.PUFFERFISH)) {
            return new Pufferfish(EntityTypes.PUFFERFISH, level);
        }
        if (display.is(Items.TROPICAL_FISH)) {
            return new TropicalFish(EntityTypes.TROPICAL_FISH, level);
        }
        return new Cod(EntityTypes.COD, level);
    }

    private static final class ActiveCatch {
        private final AbstractFish fish;
        private final Vec3 target;
        private int life;

        private ActiveCatch(AbstractFish fish, Vec3 target, int life) {
            this.fish = fish;
            this.target = target;
            this.life = life;
        }
    }
}
