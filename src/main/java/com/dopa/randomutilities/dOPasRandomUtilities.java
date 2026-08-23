package com.dopa.randomutilities;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.dopa.randomutilities.config.FeatureConfig;
import com.dopa.randomutilities.machine.config.UpgradeBonusesConfig;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(dOPasRandomUtilities.MOD_ID)
public class dOPasRandomUtilities {
    public static final String MOD_ID = "dopasrandomutilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public dOPasRandomUtilities(IEventBus modEventBus, ModContainer modContainer) {
        FeatureConfig.register(modContainer);
        UpgradeBonusesConfig.register(modContainer);
        ModSetup.register(modEventBus);
    }
}
