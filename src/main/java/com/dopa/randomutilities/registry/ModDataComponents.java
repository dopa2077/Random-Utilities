package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.FilterContents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, dOPasRandomUtilities.MOD_ID);

    /** Persistent id kept for world-save compatibility with earlier builds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterContents>> FILTER_CONTENTS =
            DATA_COMPONENTS.registerComponentType(
                    "dev_null_contents",
                    builder -> builder
                            .persistent(FilterContents.CODEC)
                            .networkSynchronized(FilterContents.STREAM_CODEC)
            );

    private ModDataComponents() {}
}
