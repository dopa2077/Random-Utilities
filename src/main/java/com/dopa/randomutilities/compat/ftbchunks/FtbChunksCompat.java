package com.dopa.randomutilities.compat.ftbchunks;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class FtbChunksCompat {
    private FtbChunksCompat() {}

    public static boolean isAvailable() {
        if (!ModList.get().isLoaded(FTBChunksAPI.MOD_ID)) {
            return false;
        }
        try {
            return FTBChunksAPI.api().isManagerLoaded();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean canEditBlock(ServerLevel level, Player actor, BlockPos pos) {
        if (!(actor instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!isAvailable()) {
            return true;
        }
        return !FTBChunksAPI.api().getManager().shouldPreventInteraction(
                serverPlayer,
                InteractionHand.MAIN_HAND,
                pos,
                Protection.EDIT_BLOCK,
                null
        );
    }
}
