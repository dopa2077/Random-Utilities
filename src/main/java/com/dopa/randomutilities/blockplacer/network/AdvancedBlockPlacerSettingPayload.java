package com.dopa.randomutilities.blockplacer.network;

import com.dopa.randomutilities.blockplacer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AdvancedBlockPlacerSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final Type<AdvancedBlockPlacerSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "advanced_block_placer_setting"));

    public static final StreamCodec<FriendlyByteBuf, AdvancedBlockPlacerSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, AdvancedBlockPlacerSettingPayload::kind,
            ByteBufCodecs.VAR_INT, AdvancedBlockPlacerSettingPayload::value,
            AdvancedBlockPlacerSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdvancedBlockPlacerSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof AdvancedBlockPlacerMenu menu) {
                menu.applySetting(payload.kind(), payload.value());
            }
        });
    }
}
