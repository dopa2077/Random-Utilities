package com.dopa.randomutilities.redstoneclock.network;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.redstoneclock.RedstoneClockMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RedstoneClockSettingPayload(byte kind, int value) implements CustomPacketPayload {
    public static final byte KIND_INTERVAL = 0;
    public static final byte KIND_PULSE = 1;
    public static final byte KIND_REDSTONE = 2;

    public static final Type<RedstoneClockSettingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "redstone_clock_setting"));

    public static final StreamCodec<FriendlyByteBuf, RedstoneClockSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, RedstoneClockSettingPayload::kind,
            ByteBufCodecs.VAR_INT, RedstoneClockSettingPayload::value,
            RedstoneClockSettingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static RedstoneClockSettingPayload interval(int value) {
        return new RedstoneClockSettingPayload(KIND_INTERVAL, value);
    }

    public static RedstoneClockSettingPayload pulse(int value) {
        return new RedstoneClockSettingPayload(KIND_PULSE, value);
    }

    public static RedstoneClockSettingPayload redstone(RedstoneMode mode) {
        return new RedstoneClockSettingPayload(KIND_REDSTONE, mode.ordinal());
    }

    public static void handle(RedstoneClockSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof RedstoneClockMenu menu)) {
                return;
            }
            switch (payload.kind()) {
                case KIND_INTERVAL -> menu.setInterval(payload.value());
                case KIND_PULSE -> menu.setPulseLength(payload.value());
                case KIND_REDSTONE -> menu.setRedstoneMode(RedstoneMode.byOrdinal(payload.value()));
                default -> {
                }
            }
        });
    }
}
