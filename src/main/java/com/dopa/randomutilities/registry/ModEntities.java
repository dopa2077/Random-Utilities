package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.config.FeatureConfig;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.block.tinytnt.PrimedTinyTnt;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, dOPasRandomUtilities.MOD_ID);

    public static DeferredHolder<EntityType<?>, EntityType<PrimedTinyTnt>> PRIMED_TINY_TNT;

    private ModEntities() {}

    public static void registerEnabled() {
        if (FeatureConfig.isBlockEnabled("tiny_tnt")) {
            PRIMED_TINY_TNT = ENTITY_TYPES.register(
                    "primed_tiny_tnt",
                    () -> EntityType.Builder.<PrimedTinyTnt>of(PrimedTinyTnt::new, MobCategory.MISC)
                            .sized(0.49F, 0.49F)
                            .eyeHeight(0.15F)
                            .clientTrackingRange(10)
                            .updateInterval(10)
                            .fireImmune()
                            .noLootTable()
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "primed_tiny_tnt")
                            ))
            );
        }
    }
}
