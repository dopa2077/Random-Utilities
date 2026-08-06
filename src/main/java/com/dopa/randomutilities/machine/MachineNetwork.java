package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.machine.network.MachineSettingPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MachineNetwork {
    private MachineNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                MachineSettingPayload.TYPE,
                MachineSettingPayload.STREAM_CODEC,
                MachineSettingPayload::handle
        );
    }
}
