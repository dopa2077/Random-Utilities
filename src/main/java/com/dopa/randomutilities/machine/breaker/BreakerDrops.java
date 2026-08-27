package com.dopa.randomutilities.machine.breaker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;

import java.util.List;

/** Dispense-style ejection used by simple and advanced breakers. */
final class BreakerDrops {
    private static final int DISPENSE_ACCURACY = 6;
    private static final double DISPENSE_OFFSET = 0.7;

    private BreakerDrops() {}

    static void ejectFromBack(ServerLevel level, BlockPos pos, Direction output, List<ItemStack> drops, boolean muteSounds) {
        ContainerOrHandler into = HopperBlockEntity.getContainerOrHandlerAt(
                level,
                pos.relative(output),
                output.getOpposite()
        );
        Vec3 dispensePos = Vec3.atCenterOf(pos).add(
                output.getStepX() * DISPENSE_OFFSET,
                output.getStepY() * DISPENSE_OFFSET,
                output.getStepZ() * DISPENSE_OFFSET
        );
        boolean shot = false;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack remaining = insertLikeDropper(into, output, drop.copy());
            if (!remaining.isEmpty()) {
                DefaultDispenseItemBehavior.spawnItem(level, remaining, DISPENSE_ACCURACY, output, dispensePos);
                shot = true;
            }
        }
        if (shot && !muteSounds) {
            level.levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, pos, 0);
            level.levelEvent(LevelEvent.PARTICLES_SHOOT_SMOKE, pos, output.get3DDataValue());
        }
    }

    private static ItemStack insertLikeDropper(ContainerOrHandler into, Direction output, ItemStack stack) {
        if (into.isEmpty()) {
            return stack;
        }
        if (into.container() != null) {
            return HopperBlockEntity.addItem(null, into.container(), stack, output.getOpposite());
        }
        return ItemUtil.insertItemReturnRemaining(into.itemHandler(), stack, false, null);
    }
}
