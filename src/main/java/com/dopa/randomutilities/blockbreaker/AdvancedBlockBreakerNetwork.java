package com.dopa.randomutilities.blockbreaker;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class AdvancedBlockBreakerNetwork {
    private AdvancedBlockBreakerNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Volume machine + ghost filter payloads registered in FilterNetwork.
    }
}
