package com.dopa.randomutilities.itemcollector;

import com.dopa.randomutilities.itemcollector.network.ItemCollectorSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ItemCollectorNetwork {
    private ItemCollectorNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ItemCollectorSettingPayload.TYPE,
                ItemCollectorSettingPayload.STREAM_CODEC,
                ItemCollectorSettingPayload::handle
        );
    }
}