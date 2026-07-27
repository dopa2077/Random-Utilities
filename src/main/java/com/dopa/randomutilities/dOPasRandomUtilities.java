package com.dopa.randomutilities;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(dOPasRandomUtilities.MOD_ID)
public class dOPasRandomUtilities {
    public static final String MOD_ID = "dopasrandomutilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public dOPasRandomUtilities(IEventBus modEventBus, ModContainer modContainer) {
        ModSetup.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Basic Stone Generator recipes loaded: {} entries", com.dopa.randomutilities.config.GeneratorRecipeConfig.getRecipes().size());
    }
}
