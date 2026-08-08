package com.dopa.randomutilities.fishnet.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.fishnet.FishnetNetwork;
import com.dopa.randomutilities.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class FishnetClient {
    private FishnetClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        FishnetNetwork.clientCatchHandler = payload -> FishnetCatchEffects.play(payload.pos(), payload.display());
        event.register(ModMenus.FISHNET.get(), FishnetScreen::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        FishnetCatchEffects.clientTick();
    }
}
