package com.dopa.randomutilities.item.magnet.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.item.magnet.menu.MagnetMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MagnetSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final byte KIND_RANGE = 0;
    public static final byte KIND_FILTER_MODE = 1;
    public static final byte KIND_COLLECT = 2;
    public static final byte KIND_IGNORE_DELAY = 3;
    public static final byte KIND_PAUSE_SNEAK = 4;
    public static final byte KIND_PULL_XP = 5;
    public static final byte KIND_PARTICLES = 6;
    public static final byte KIND_COLOR = 7;

    public static final Type<MagnetSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "item_magnet_setting"));

    public static final StreamCodec<FriendlyByteBuf, MagnetSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, MagnetSettingPayload::kind,
            ByteBufCodecs.VAR_INT, MagnetSettingPayload::value,
            MagnetSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MagnetSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof MagnetMenu menu) || !menu.stillValid(player)) {
                return;
            }
            switch (payload.kind()) {
                case KIND_RANGE -> menu.setRange(payload.value());
                case KIND_FILTER_MODE -> menu.setWhitelistMode(payload.value() != 0);
                case KIND_COLLECT -> menu.setCollectMode(payload.value() != 0);
                case KIND_IGNORE_DELAY -> menu.setIgnorePickupDelay(payload.value() != 0);
                case KIND_PAUSE_SNEAK -> menu.setPauseOnSneak(payload.value() != 0);
                case KIND_PULL_XP -> menu.setPullXp(payload.value() != 0);
                case KIND_PARTICLES -> menu.setParticlesEnabled(payload.value() != 0);
                case KIND_COLOR -> menu.setOverlayColor(payload.value());
                default -> {
                }
            }
        });
    }
}
