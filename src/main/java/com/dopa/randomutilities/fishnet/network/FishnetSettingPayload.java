package com.dopa.randomutilities.fishnet.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.fishnet.menu.FishnetMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FishnetSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final byte KIND_PARTICLES = 0;
    public static final byte KIND_SOUND = 1;

    public static final Type<FishnetSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "fishnet_setting"));

    public static final StreamCodec<FriendlyByteBuf, FishnetSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, FishnetSettingPayload::kind,
            ByteBufCodecs.VAR_INT, FishnetSettingPayload::value,
            FishnetSettingPayload::new
    );

    public static FishnetSettingPayload particles(boolean enabled) {
        return new FishnetSettingPayload(KIND_PARTICLES, enabled ? 1 : 0);
    }

    public static FishnetSettingPayload sound(boolean enabled) {
        return new FishnetSettingPayload(KIND_SOUND, enabled ? 1 : 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FishnetSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof FishnetMenu menu) || !menu.stillValid(player)) {
                return;
            }
            switch (payload.kind()) {
                case KIND_PARTICLES -> menu.setParticlesEnabled(payload.value() != 0);
                case KIND_SOUND -> menu.setSoundEnabled(payload.value() != 0);
                default -> {
                }
            }
        });
    }
}
