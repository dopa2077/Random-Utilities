package com.dopa.randomutilities.fishnet;

import com.dopa.randomutilities.fishnet.network.FishnetApproachPayload;
import com.dopa.randomutilities.fishnet.network.FishnetSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

public final class FishnetNetwork {
    /** Set from client setup; no-op on dedicated server. */
    public static Consumer<FishnetApproachPayload> clientApproachHandler = payload -> {};

    private FishnetNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                FishnetApproachPayload.TYPE,
                FishnetApproachPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientApproachHandler.accept(payload))
        );
        registrar.playToServer(
                FishnetSettingPayload.TYPE,
                FishnetSettingPayload.STREAM_CODEC,
                FishnetSettingPayload::handle
        );
    }
}
