package com.dopa.randomutilities.itemcollector;

import com.dopa.randomutilities.itemcollector.config.ItemCollectorConfig;
import com.dopa.randomutilities.util.GhostItemFilter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** World item pickup and insertion for {@link ItemCollectorBlockEntity}. */
final class ItemCollectorLogic {
    private ItemCollectorLogic() {}

    static void tick(ServerLevel level, BlockPos pos, BlockState state, ItemCollectorBlockEntity be) {
        if (!be.redstoneMode().allowsOperation(level.getBestNeighborSignal(pos))) {
            return;
        }
        int delay = be.pickupDelay();
        if (delay == Integer.MAX_VALUE) {
            return;
        }
        if (++be.tickCounter < delay) {
            return;
        }
        be.tickCounter = 0;

        Direction facing = state.getValue(ItemCollectorBlock.FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        ResourceHandler<ItemResource> handler = getItemHandler(level, supportPos, facing);
        if (handler == null) {
            return;
        }

        AABB scanBox = be.scanBox();
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, scanBox);
        if (entities.isEmpty()) {
            return;
        }

        ItemCollectorType type = be.collectorType();
        int remaining = be.pickupBatch();

        for (ItemEntity entity : entities) {
            if (remaining <= 0) {
                break;
            }
            if (!entity.isAlive() || entity.getItem().isEmpty()) {
                continue;
            }
            ItemStack stack = entity.getItem();
            if (!GhostItemFilter.allows(stack, be.filterSlots(), be.whitelistMode())) {
                continue;
            }
            if (type.supportsLineOfSight()
                    && ItemCollectorConfig.lineOfSightEnabled()
                    && be.requireLineOfSight()
                    && !hasLineOfSight(level, pos, facing, entity)) {
                continue;
            }

            int moved = tryInsert(handler, stack, remaining, true);
            if (moved <= 0) {
                continue;
            }
            moved = tryInsert(handler, stack, moved, false);
            if (moved <= 0) {
                continue;
            }

            stack.shrink(moved);
            if (stack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            remaining -= moved;

            if (be.particlesEnabled()) {
                Vec3 particlePos = entity.position();
                level.sendParticles(
                        ParticleTypes.PORTAL,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        3,
                        0.1,
                        0.1,
                        0.1,
                        0.02
                );
            }
        }
    }

    @Nullable
    private static ResourceHandler<ItemResource> getItemHandler(ServerLevel level, BlockPos supportPos, Direction facing) {
        return level.getCapability(Capabilities.Item.BLOCK, supportPos, facing);
    }

    private static int tryInsert(
            ResourceHandler<ItemResource> handler,
            ItemStack stack,
            int maxAmount,
            boolean simulate
    ) {
        ItemResource resource = ItemResource.of(stack);
        if (resource.isEmpty() || maxAmount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.open(null)) {
            int inserted = handler.insert(resource, Math.min(maxAmount, stack.getCount()), tx);
            if (inserted > 0 && !simulate) {
                tx.commit();
            }
            return Math.max(0, inserted);
        }
    }

    /** Closes zero-width corner/edge cracks that thin Minecraft clips miss. */
    private static final double LOS_THICKNESS = 0.05;

    /**
     * Conservative LOS from collector tip to the item: center + mid-height AABB corners
     * must all be clear. Each ray uses a thick swept probe and inflated colliders so
     * diagonal grazes through shared block edges cannot slip through.
     */
    private static boolean hasLineOfSight(ServerLevel level, BlockPos collectorPos, Direction facing, ItemEntity entity) {
        Vec3 from = Vec3.atCenterOf(collectorPos).add(
                facing.getStepX() * 0.45,
                facing.getStepY() * 0.45,
                facing.getStepZ() * 0.45
        );
        AABB itemBox = entity.getBoundingBox();
        double midY = (itemBox.minY + itemBox.maxY) * 0.5;
        Vec3[] targets = {
                itemBox.getCenter(),
                new Vec3(itemBox.minX, midY, itemBox.minZ),
                new Vec3(itemBox.maxX, midY, itemBox.minZ),
                new Vec3(itemBox.minX, midY, itemBox.maxZ),
                new Vec3(itemBox.maxX, midY, itemBox.maxZ)
        };
        for (Vec3 to : targets) {
            if (!isRayClear(level, collectorPos, from, to, itemBox)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRayClear(
            ServerLevel level,
            BlockPos collectorPos,
            Vec3 from,
            Vec3 to,
            AABB itemBox
    ) {
        AABB probe = new AABB(to, to).inflate(LOS_THICKNESS);
        double reachSq = from.distanceToSqr(to);
        return BlockGetter.forEachBlockIntersectedBetween(from, to, probe, (pos, iteration) -> {
            if (pos.equals(collectorPos)) {
                return true;
            }
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                return true;
            }
            for (AABB part : shape.toAabbs()) {
                AABB world = part.move(pos);
                // Floor / support under the item is not an obstruction.
                if (itemBox.intersects(world)) {
                    continue;
                }
                AABB inflated = world.inflate(LOS_THICKNESS);
                if (inflated.contains(from)) {
                    return false;
                }
                var hit = inflated.clip(from, to);
                if (hit.isPresent() && from.distanceToSqr(hit.get()) < reachSq - 1.0E-4) {
                    return false;
                }
            }
            return true;
        });
    }
}
