package com.dopa.randomutilities.filter;

import com.dopa.randomutilities.filter.network.FilterSelectPayload;
import com.dopa.randomutilities.filter.network.FilterSettingPayload;
import com.dopa.randomutilities.filter.network.GhostFilterPayload;
import com.dopa.randomutilities.gui.machine.network.VolumeMachineSettingPayload;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FilterNetwork {
    private FilterNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(FilterSettingPayload.TYPE, FilterSettingPayload.STREAM_CODEC, FilterSettingPayload::handle);
        registrar.playToServer(FilterSelectPayload.TYPE, FilterSelectPayload.STREAM_CODEC, FilterSelectPayload::handle);
        registrar.playToServer(GhostFilterPayload.TYPE, GhostFilterPayload.STREAM_CODEC, GhostFilterPayload::handle);
        registrar.playToServer(
                VolumeMachineSettingPayload.TYPE,
                VolumeMachineSettingPayload.STREAM_CODEC,
                VolumeMachineSettingPayload::handle
        );
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        var items = FilterRegistry.allItems();
        if (items.length == 0) {
            return;
        }
        event.registerItem(
                Capabilities.Item.ITEM,
                (stack, access) -> new FilterItemHandler(access),
                items
        );
    }
}
