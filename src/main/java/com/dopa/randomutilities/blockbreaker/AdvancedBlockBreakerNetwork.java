package com.dopa.randomutilities.blockbreaker;

import com.dopa.randomutilities.blockbreaker.network.AdvancedBlockBreakerFilterPayload;
import com.dopa.randomutilities.blockbreaker.network.AdvancedBlockBreakerSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AdvancedBlockBreakerNetwork {
    private AdvancedBlockBreakerNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                AdvancedBlockBreakerSettingPayload.TYPE,
                AdvancedBlockBreakerSettingPayload.STREAM_CODEC,
                AdvancedBlockBreakerSettingPayload::handle
        );
        registrar.playToServer(
                AdvancedBlockBreakerFilterPayload.TYPE,
                AdvancedBlockBreakerFilterPayload.STREAM_CODEC,
                AdvancedBlockBreakerFilterPayload::handle
        );
    }
}
