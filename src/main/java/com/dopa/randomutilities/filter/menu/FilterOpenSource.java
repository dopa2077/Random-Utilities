package com.dopa.randomutilities.filter.menu;

import com.dopa.randomutilities.filter.FilterContents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Encodes how a filter menu was opened (held item vs block host). */
record FilterOpenSource(
        @Nullable InteractionHand hand,
        @Nullable BlockPos blockPos,
        @Nullable FilterContents presetContents,
        boolean restoreConfig
) {
    static FilterOpenSource forHand(Player player, InteractionHand hand,
                                    @Nullable FilterContents preset, boolean restoreConfig) {
        return new FilterOpenSource(hand, null, preset, restoreConfig);
    }

    static FilterOpenSource forBlock(BlockPos pos, FilterContents contents, boolean restoreConfig) {
        return new FilterOpenSource(null, pos, contents, restoreConfig);
    }

    static FilterOpenSource decode(Player player, RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            BlockPos pos = buf.readBlockPos();
            FilterContents contents = FilterContents.STREAM_CODEC.decode(buf);
            boolean restore = buf.readBoolean();
            return forBlock(pos, contents, restore);
        }
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        FilterContents contents = null;
        boolean restore = false;
        if (buf.readBoolean()) {
            contents = FilterContents.STREAM_CODEC.decode(buf);
            restore = buf.readBoolean();
        }
        return forHand(player, hand, contents, restore);
    }
}
