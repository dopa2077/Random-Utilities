package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.DevNullNestTrigger;

import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, dOPasRandomUtilities.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, DevNullNestTrigger> DEV_NULL_NEST =
            TRIGGER_TYPES.register("dev_null_nest", DevNullNestTrigger::new);

    private ModTriggers() {}
}
