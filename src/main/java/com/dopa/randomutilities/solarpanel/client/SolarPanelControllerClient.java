package com.dopa.randomutilities.solarpanel.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class SolarPanelControllerClient {
    private SolarPanelControllerClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SOLAR_PANEL_CONTROLLER.get(), SolarPanelControllerScreen::new);
    }
}
