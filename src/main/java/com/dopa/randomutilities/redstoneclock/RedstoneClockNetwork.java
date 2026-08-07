package com.dopa.randomutilities.redstoneclock;

import com.dopa.randomutilities.redstoneclock.network.RedstoneClockSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RedstoneClockNetwork {
    private RedstoneClockNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RedstoneClockSettingPayload.TYPE,
                RedstoneClockSettingPayload.STREAM_CODEC,
                RedstoneClockSettingPayload::handle
        );
    }
}
