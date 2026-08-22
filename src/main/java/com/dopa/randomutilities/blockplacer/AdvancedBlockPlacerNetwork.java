package com.dopa.randomutilities.blockplacer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class AdvancedBlockPlacerNetwork {
    private AdvancedBlockPlacerNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Volume machine + ghost filter payloads registered in FilterNetwork.
    }
}
