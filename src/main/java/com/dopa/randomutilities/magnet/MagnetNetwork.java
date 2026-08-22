package com.dopa.randomutilities.magnet;

import com.dopa.randomutilities.magnet.network.MagnetSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MagnetNetwork {
    private MagnetNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                MagnetSettingPayload.TYPE,
                MagnetSettingPayload.STREAM_CODEC,
                MagnetSettingPayload::handle
        );
    }
}
