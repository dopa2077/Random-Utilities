package com.dopa.randomutilities.transfer.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.transfer.menu.TransferNodeMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TransferNodeSettingPayload(boolean whitelistMode) implements CustomPacketPayload {
    public static final Type<TransferNodeSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "transfer_node_setting"));

    public static final StreamCodec<FriendlyByteBuf, TransferNodeSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TransferNodeSettingPayload::whitelistMode,
            TransferNodeSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TransferNodeSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TransferNodeMenu menu && menu.stillValid(player)) {
                menu.setWhitelistMode(payload.whitelistMode());
            }
        });
    }
}
