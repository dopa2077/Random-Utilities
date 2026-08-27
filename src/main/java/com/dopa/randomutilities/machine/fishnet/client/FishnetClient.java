package com.dopa.randomutilities.machine.fishnet.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.machine.fishnet.network.FishnetApproachPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class FishnetClient {
    private FishnetClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FishnetApproachPayload.clientHandler = payload -> FishnetCatchEffects.play(payload.pos(), payload.durationTicks());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        FishnetCatchEffects.clientTick();
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        FishnetCatchEffects.clear();
    }
}
