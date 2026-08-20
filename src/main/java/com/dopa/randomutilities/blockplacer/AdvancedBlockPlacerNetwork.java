package com.dopa.randomutilities.blockplacer;

import com.dopa.randomutilities.blockplacer.network.AdvancedBlockPlacerFilterPayload;
import com.dopa.randomutilities.blockplacer.network.AdvancedBlockPlacerSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AdvancedBlockPlacerNetwork {
    private AdvancedBlockPlacerNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                AdvancedBlockPlacerSettingPayload.TYPE,
                AdvancedBlockPlacerSettingPayload.STREAM_CODEC,
                AdvancedBlockPlacerSettingPayload::handle
        );
        registrar.playToServer(
                AdvancedBlockPlacerFilterPayload.TYPE,
                AdvancedBlockPlacerFilterPayload.STREAM_CODEC,
                AdvancedBlockPlacerFilterPayload::handle
        );
    }
}
