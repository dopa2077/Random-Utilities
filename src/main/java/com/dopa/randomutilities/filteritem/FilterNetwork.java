package com.dopa.randomutilities.filteritem;

import com.dopa.randomutilities.filteritem.network.FilterSelectPayload;
import com.dopa.randomutilities.filteritem.network.FilterSettingPayload;

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
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.Item.ITEM,
                (stack, access) -> new FilterItemHandler(access),
                FilterRegistry.allItems()
        );
    }
}
