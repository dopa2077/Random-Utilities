package com.dopa.randomutilities.gui.machine.network;

import com.dopa.randomutilities.blockbreaker.menu.AdvancedBlockBreakerMenu;
import com.dopa.randomutilities.blockplacer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VolumeMachineSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final Type<VolumeMachineSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "volume_machine_setting"));

    public static final StreamCodec<FriendlyByteBuf, VolumeMachineSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, VolumeMachineSettingPayload::kind,
            ByteBufCodecs.VAR_INT, VolumeMachineSettingPayload::value,
            VolumeMachineSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VolumeMachineSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof AdvancedBlockBreakerMenu breaker && breaker.stillValid(player)) {
                breaker.applySetting(payload.kind(), payload.value());
            } else if (player.containerMenu instanceof AdvancedBlockPlacerMenu placer && placer.stillValid(player)) {
                placer.applySetting(payload.kind(), payload.value());
            }
        });
    }
}
