package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, dOPasRandomUtilities.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TINY_TNT_EXPLODE = register("tiny_tnt.explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> TINY_TNT_PRIMED = register("tiny_tnt.primed");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
