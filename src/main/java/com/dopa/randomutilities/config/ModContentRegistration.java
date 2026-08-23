package com.dopa.randomutilities.config;

import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModEntities;
import com.dopa.randomutilities.registry.ModItems;

/**
 * Registers mod blocks/items once startup config has been loaded and before deferred registers
 * subscribe to the mod event bus (see {@link com.dopa.randomutilities.ModSetup#register}).
 */
public final class ModContentRegistration {
    private static boolean registered;

    private ModContentRegistration() {}

    public static void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        ModBlocks.registerEnabled();
        ModItems.registerEnabled();
        ModBlockEntities.registerEnabled();
        ModEntities.registerEnabled();
    }
}
