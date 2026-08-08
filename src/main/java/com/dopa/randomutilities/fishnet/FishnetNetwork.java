package com.dopa.randomutilities.fishnet;

import com.dopa.randomutilities.fishnet.network.FishnetCatchPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

public final class FishnetNetwork {
    /** Set from client setup; no-op on dedicated server. */
    public static Consumer<FishnetCatchPayload> clientCatchHandler = payload -> {};

    private FishnetNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                FishnetCatchPayload.TYPE,
                FishnetCatchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientCatchHandler.accept(payload))
        );
    }
}
