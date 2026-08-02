package com.dopa.randomutilities.item.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filteritem.client.FilterDecorator;
import com.dopa.randomutilities.registry.ModItems;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/** Client registration for /dev/null items. Shared filter client code lives in {@code filteritem.client}. */
@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class DevNullClient {
    private DevNullClient() {}

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModItems.DEV_NULL.get(), FilterDecorator.INSTANCE);
        event.register(ModItems.ADVANCED_DEV_NULL.get(), FilterDecorator.INSTANCE);
    }
}
