package com.dopa.randomutilities.transfer;

import com.dopa.randomutilities.transfer.network.TransferNodeSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TransferNodeNetwork {
    private TransferNodeNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                TransferNodeSettingPayload.TYPE,
                TransferNodeSettingPayload.STREAM_CODEC,
                TransferNodeSettingPayload::handle
        );
    }
}
