package com.dopa.randomutilities.blockbreaker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Multi-hit breaking for hard blocks. Obsidian (hardness 50) takes 4 unenchanted hits;
 * each Efficiency level removes one hit, down to 1 at Efficiency V. Extra energy is based
 * on that unenchanted hit count, so Efficiency does not cheapen obsidian.
 */
public final class BreakerMining {
    public static final float REFERENCE_HARDNESS = 50.0F;
    public static final int REFERENCE_HITS = 4;

    private @Nullable BlockPos target;
    private int hits;

    public static int baseHits(float hardness) {
        if (hardness <= 0.0F) {
            return 1;
        }
        return Math.max(1, Mth.ceil(hardness * REFERENCE_HITS / REFERENCE_HARDNESS));
    }

    public static int hitsNeeded(float hardness, int efficiency) {
        return Math.max(1, baseHits(hardness) - Math.max(0, efficiency));
    }

    public static int energyMultiplier(float hardness) {
        return baseHits(hardness);
    }

    public static int efficiency(ServerLevel level, ItemStack tool) {
        if (tool.isEmpty()) {
            return 0;
        }
        Holder<Enchantment> enchantment = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY);
        return tool.getEnchantmentLevel(enchantment);
    }

    public static void playHit(ServerLevel level, BlockPos pos, BlockState state) {
        SoundType sound = state.getSoundType();
        level.playSound(
                null,
                pos,
                sound.getHitSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 8.0F,
                sound.getPitch() * 0.5F
        );
    }

    public @Nullable BlockPos target() {
        return target;
    }

    public void clear(ServerLevel level, int breakerId) {
        if (target != null) {
            level.destroyBlockProgress(breakerId, target, -1);
        }
        target = null;
        hits = 0;
    }

    /**
     * Records one hit. Returns {@code true} when this hit should break the block.
     */
    public boolean completeAfterHit(ServerLevel level, int breakerId, BlockPos pos, int hitsNeeded) {
        if (target == null || !target.equals(pos)) {
            clear(level, breakerId);
            target = pos.immutable();
        }
        hits++;
        if (hits >= Math.max(1, hitsNeeded)) {
            clear(level, breakerId);
            return true;
        }
        level.destroyBlockProgress(breakerId, target, crackStage(hits));
        return false;
    }

    public void save(ValueOutput output) {
        if (target == null || hits <= 0) {
            return;
        }
        output.putInt("MiningX", target.getX());
        output.putInt("MiningY", target.getY());
        output.putInt("MiningZ", target.getZ());
        output.putInt("MiningHits", hits);
    }

    public void load(ValueInput input) {
        hits = Math.max(0, input.getIntOr("MiningHits", 0));
        if (hits <= 0) {
            target = null;
            return;
        }
        target = new BlockPos(
                input.getIntOr("MiningX", 0),
                input.getIntOr("MiningY", 0),
                input.getIntOr("MiningZ", 0)
        );
    }

    /** Skip early chips: hit 1→4, 2→6, 3→8, then the block breaks. */
    private static int crackStage(int hitsDone) {
        return Mth.clamp(2 + hitsDone * 2, 0, 9);
    }
}
