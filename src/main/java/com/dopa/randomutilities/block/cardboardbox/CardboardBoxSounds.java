package com.dopa.randomutilities.block.cardboardbox;

import com.dopa.randomutilities.registry.ModSounds;

import net.minecraft.world.level.block.SoundType;

final class CardboardBoxSounds {
    private static SoundType blockSoundType;

    private CardboardBoxSounds() {}

    static SoundType blockSoundType() {
        if (blockSoundType == null) {
            blockSoundType = new SoundType(
                    0.85F,
                    1.0F,
                    SoundType.WOOL.getBreakSound(),
                    SoundType.WOOL.getStepSound(),
                    ModSounds.CARDBOARD_BOX_PACK.get(),
                    SoundType.WOOL.getHitSound(),
                    SoundType.WOOL.getFallSound()
            );
        }
        return blockSoundType;
    }
}
