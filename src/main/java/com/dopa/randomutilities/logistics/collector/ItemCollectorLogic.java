package com.dopa.randomutilities.logistics.collector;

import com.dopa.randomutilities.core.util.GhostItemFilter;

import com.dopa.randomutilities.core.machine.config.UpgradeConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
        if (be.emptySweepBackoff() > 0) {
            be.tickEmptySweepBackoff();
            return;
        }

        Direction facing = state.getValue(ItemCollectorBlock.FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        ResourceHandler<ItemResource> handler = getItemHandler(level, supportPos, facing);
        if (handler == null) {
            return;
        }

        AABB scanBox = be.scanBox();
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, scanBox);
        if (entities.isEmpty()) {
            be.onEmptySweep();
            return;
        }

        int stackCount = be.upgrades().stackCount();
        int remaining = UpgradeConfig.stackCollectorTotal(be.pickupBatch(), stackCount);
        boolean mixTypes = UpgradeConfig.stackAllowsMixedTypes(stackCount);
        Item lockedType = null;
        boolean movedAny = false;

        for (ItemEntity entity : entities) {
            if (remaining <= 0) {
                break;
            }
            if (!entity.isAlive() || entity.getItem().isEmpty() || entity.hasPickUpDelay()) {
                continue;
            }
            ItemStack stack = entity.getItem();
            if (!GhostItemFilter.allows(stack, be.filterSlots(), be.whitelistMode())) {
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

            int moved = tryInsert(handler, stack, remaining);
            if (moved <= 0) {
                continue;
            }
            movedAny = true;

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
        if (!movedAny) {
            be.onEmptySweep();
        } else {
            be.onSuccessfulSweep();
        }
    }

    @Nullable
    private static ResourceHandler<ItemResource> getItemHandler(ServerLevel level, BlockPos supportPos, Direction facing) {
        return level.getCapability(Capabilities.Item.BLOCK, supportPos, facing);
    }

    private static int tryInsert(
            ResourceHandler<ItemResource> handler,
            ItemStack stack,
            int maxAmount
    ) {
        ItemResource resource = ItemResource.of(stack);
        if (resource.isEmpty() || maxAmount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.open(null)) {
            int inserted = handler.insert(resource, Math.min(maxAmount, stack.getCount()), tx);
            if (inserted > 0) {
                tx.commit();
            }
            return Math.max(0, inserted);
        }
    }
}
