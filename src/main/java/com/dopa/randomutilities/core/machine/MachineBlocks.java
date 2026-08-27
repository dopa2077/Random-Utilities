package com.dopa.randomutilities.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared shift-click upgrade insert for machine blocks. */
public final class MachineBlocks {
    private MachineBlocks() {}

    public static InteractionResult tryInsertUpgrade(
            Player player,
            ItemStack stack,
            Level level,
            BlockPos pos,
            UpgradeInventory upgrades
    ) {
        if (!player.isShiftKeyDown() || !upgrades.accepts(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int inserted = upgrades.insertFrom(stack);
        if (inserted <= 0) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(inserted);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.1F);
        return InteractionResult.CONSUME;
    }
}
