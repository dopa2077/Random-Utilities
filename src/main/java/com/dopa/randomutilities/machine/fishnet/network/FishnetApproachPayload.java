package com.dopa.randomutilities.machine.fishnet.network;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public record FishnetApproachPayload(BlockPos pos, int durationTicks) implements CustomPacketPayload {
    public static final Type<FishnetApproachPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "fishnet_approach"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FishnetApproachPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FishnetApproachPayload::pos,
                    ByteBufCodecs.VAR_INT, FishnetApproachPayload::durationTicks,
                    FishnetApproachPayload::new
            );

    /** Set from client setup; no-op on dedicated server. */
    public static Consumer<FishnetApproachPayload> clientHandler = payload -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
