package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.compat.ftbchunks.FtbChunksCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

public final class ClaimActionGate {
    private ClaimActionGate() {}

    public static boolean canBreak(ServerLevel level, Player actor, BlockPos pos) {
        if (FtbChunksCompat.isAvailable()) {
            return FtbChunksCompat.canEditBlock(level, actor, pos);
        }
        return probeBreakEvent(level, actor, pos);
    }

    public static boolean canPlace(ServerLevel level, Player actor, BlockPos pos, ItemStack stack) {
        if (FtbChunksCompat.isAvailable()) {
            return FtbChunksCompat.canEditBlock(level, actor, pos);
        }
        return probePlaceEvent(level, actor, pos, stack);
    }

    private static boolean probeBreakEvent(ServerLevel level, Player actor, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        GameType gameType = actor instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                ? serverPlayer.gameMode.getGameModeForPlayer()
                : GameType.SURVIVAL;
        return !CommonHooks.fireBlockBreak(level, gameType, actor, pos, state).isCanceled();
    }

    private static boolean probePlaceEvent(ServerLevel level, Player actor, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return false;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        boolean canceled = EventHooks.onBlockPlace(actor, snapshot, net.minecraft.core.Direction.UP);
        return !canceled;
    }
}
