package com.dopa.randomutilities.fishnet.network;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record FishnetCatchPayload(BlockPos pos, ItemStack display) implements CustomPacketPayload {
    public static final Type<FishnetCatchPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "fishnet_catch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FishnetCatchPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FishnetCatchPayload::pos,
            ItemStack.STREAM_CODEC, FishnetCatchPayload::display,
            FishnetCatchPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
