package com.dopa.randomutilities.trashcan;

import com.dopa.randomutilities.trashcan.network.TrashCanSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TrashCanNetwork {
    private TrashCanNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                TrashCanSettingPayload.TYPE,
                TrashCanSettingPayload.STREAM_CODEC,
                TrashCanSettingPayload::handle
        );
    }
}
