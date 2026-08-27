package com.dopa.randomutilities.machine.placer;

import com.dopa.randomutilities.core.machine.MachineActors;
import com.dopa.randomutilities.core.util.BlockOrientations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/** Shared analog, slot pick, and BlockItem place sequence for simple/advanced placers. */
final class BlockPlacerActions {
    private BlockPlacerActions() {}

    static int analogOutput(int slotCount, IntFunction<ItemStack> stackInSlot) {
        float fill = 0.0F;
        boolean any = false;
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = stackInSlot.apply(i);
            if (!stack.isEmpty()) {
                any = true;
                fill += (float) stack.getCount() / (float) Math.max(1, stack.getMaxStackSize());
            }
        }
        return any ? Mth.floor(fill / slotCount * 14.0F) + 1 : 0;
    }

    static int pickRandomBlockSlot(
            int slotCount,
            RandomSource random,
            IntFunction<ItemStack> stackInSlot,
            Predicate<ItemStack> allow
    ) {
        int chosen = -1;
        int seen = 0;
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = stackInSlot.apply(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem) || !allow.test(stack)) {
                continue;
            }
            seen++;
            if (random.nextInt(seen) == 0) {
                chosen = i;
            }
        }
        return chosen;
    }

    static boolean placeFromSlot(
            ServerLevel level,
            BlockPos machinePos,
            BlockState machineState,
            BlockPos target,
            int slot,
            ItemStacksResourceHandler items,
            IntFunction<ItemStack> stackInSlot,
            UUID ownerUuid,
            java.util.function.BiPredicate<FakePlayer, ItemStack> canPlace
    ) {
        Direction facing = BlockOrientations.front(machineState);
        ItemStack stack = stackInSlot.apply(slot);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        FakePlayer placer = MachineActors.actor(level, ownerUuid, machinePos, facing).orElse(null);
        if (placer == null) {
            return false;
        }
        if (!canPlace.test(placer, stack)) {
            return false;
        }
        ItemStack held = stack.copy();
        placer.setItemInHand(InteractionHand.MAIN_HAND, held);
        Vec3 hitLoc = Vec3.atCenterOf(target).subtract(
                facing.getStepX() * 0.5,
                facing.getStepY() * 0.5,
                facing.getStepZ() * 0.5
        );
        BlockHitResult hit = new BlockHitResult(hitLoc, facing.getOpposite(), target, false);
        BlockPlaceContext context = new BlockPlaceContext(placer, InteractionHand.MAIN_HAND, held, hit);
        InteractionResult result = blockItem.place(context);
        ItemStack remaining = placer.getItemInHand(InteractionHand.MAIN_HAND);
        if (remaining.isEmpty()) {
            items.set(slot, ItemResource.EMPTY, 0);
        } else {
            items.set(slot, ItemResource.of(remaining), remaining.getCount());
        }
        placer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return result.consumesAction();
    }
}
