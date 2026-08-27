package com.dopa.randomutilities.block.trashcan.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.block.trashcan.TrashCanMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrashCanSettingPayload(boolean whitelistMode) implements CustomPacketPayload {
    public static final Type<TrashCanSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "trash_can_setting"));

    public static final StreamCodec<FriendlyByteBuf, TrashCanSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TrashCanSettingPayload::whitelistMode,
            TrashCanSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrashCanSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TrashCanMenu menu && menu.stillValid(player)) {
                menu.setWhitelistMode(payload.whitelistMode());
            }
        });
    }
}
