package com.dopa.randomutilities.machine.breaker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Collects item drops spawned by a break without vacuuming older nearby entities. */
final class BreakerDropCapture {
    /** Only entities spawned this tick or the next are treated as break drops. */
    private static final int MAX_ITEM_AGE = 2;

    private BreakerDropCapture() {}

    static void collectFresh(
            ServerLevel level,
            AABB area,
            Set<UUID> existing,
            List<ItemStack> drops
    ) {
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (existing.contains(entity.getUUID())) {
                continue;
            }
            if (entity.getAge() > MAX_ITEM_AGE) {
                continue;
            }
            ItemStack stack = entity.getItem();
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
            entity.discard();
        }
    }
}
