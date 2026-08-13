package com.dopa.randomutilities.filter.menu;

import com.dopa.randomutilities.filter.FilterContents;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

/** Encodes how a filter menu was opened from a held item. */
record FilterOpenSource(
        InteractionHand hand,
        @Nullable FilterContents presetContents,
        boolean restoreConfig
) {
    static FilterOpenSource forHand(InteractionHand hand,
                                    @Nullable FilterContents preset, boolean restoreConfig) {
        return new FilterOpenSource(hand, preset, restoreConfig);
    }

    static FilterOpenSource decode(RegistryFriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        FilterContents contents = null;
        boolean restore = false;
        if (buf.readBoolean()) {
            contents = FilterContents.STREAM_CODEC.decode(buf);
            restore = buf.readBoolean();
        }
        return forHand(hand, contents, restore);
    }
}
