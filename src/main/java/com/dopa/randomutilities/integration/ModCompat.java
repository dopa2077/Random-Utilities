package com.dopa.randomutilities.integration;

import com.dopa.randomutilities.integration.top.TopCompat;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

public final class ModCompat {
    private ModCompat() {}

    public static void onInterModEnqueue(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("theoneprobe")) {
            InterModComms.sendTo("theoneprobe", "getTheOneProbe", TopCompat::createPlugin);
        }
    }
}
