package com.dopa.randomutilities.blockbreaker.network;

import com.dopa.randomutilities.blockbreaker.menu.AdvancedBlockBreakerMenu;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AdvancedBlockBreakerSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final Type<AdvancedBlockBreakerSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "advanced_block_breaker_setting"));

    public static final StreamCodec<FriendlyByteBuf, AdvancedBlockBreakerSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, AdvancedBlockBreakerSettingPayload::kind,
            ByteBufCodecs.VAR_INT, AdvancedBlockBreakerSettingPayload::value,
            AdvancedBlockBreakerSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdvancedBlockBreakerSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof AdvancedBlockBreakerMenu menu && menu.stillValid(player)) {
                menu.applySetting(payload.kind(), payload.value());
            }
        });
    }
}
