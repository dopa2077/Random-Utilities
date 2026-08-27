package com.dopa.randomutilities.item.magnet.client;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class MagnetClient {
    private MagnetClient() {}

    @SubscribeEvent
    public static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "magnet_color"),
                MagnetTintSource.MAP_CODEC
        );
    }
}
