package com.dopa.randomutilities.core.machine;

import com.dopa.randomutilities.integration.ftbchunks.FtbChunksCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;

public final class ClaimActionGate {
    private ClaimActionGate() {}

    public static boolean canBreak(ServerLevel level, Player actor, BlockPos pos) {
        if (FtbChunksCompat.isAvailable()) {
            return FtbChunksCompat.canEditBlock(level, actor, pos);
        }
        return probeBreakEvent(level, actor, pos);
    }

    public static boolean canPlace(ServerLevel level, Player actor, BlockPos pos, ItemStack stack, Direction face) {
        if (FtbChunksCompat.isAvailable()) {
            return FtbChunksCompat.canEditBlock(level, actor, pos);
        }
        return probePlaceEvent(level, actor, pos, stack, face);
    }

    public static boolean canPlace(ServerLevel level, Player actor, BlockPos pos, ItemStack stack) {
        return canPlace(level, actor, pos, stack, Direction.UP);
    }

    /** Whether automation may insert into an inventory at {@code destPos}. */
    public static boolean canInsertInto(ServerLevel level, BlockPos destPos) {
        Player actor = FakePlayerFactory.getMinecraft(level);
        if (FtbChunksCompat.isAvailable()) {
            return FtbChunksCompat.canEditBlock(level, actor, destPos);
        }
        return true;
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

    private static boolean probePlaceEvent(ServerLevel level, Player actor, BlockPos pos, ItemStack stack, Direction face) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return false;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        boolean canceled = EventHooks.onBlockPlace(actor, snapshot, face);
        return !canceled;
    }
}
