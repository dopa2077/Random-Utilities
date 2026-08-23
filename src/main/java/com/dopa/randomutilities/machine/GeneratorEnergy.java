package com.dopa.randomutilities.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Pushes stored generator FE into adjacent energy capabilities. */
public final class GeneratorEnergy {
    private static final Direction[] DIRECTIONS = Direction.values();

    private GeneratorEnergy() {}

    public static void pushToNeighbors(Level level, BlockPos pos, MachineEnergy energy) {
        if (level.isClientSide()) {
            return;
        }
        int budget = Math.min(energy.stored(), energy.maxExtractRate());
        if (budget <= 0) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : DIRECTIONS) {
            if (budget <= 0) {
                break;
            }
            cursor.setWithOffset(pos, direction);
            EnergyHandler dest = level.getCapability(
                    Capabilities.Energy.BLOCK,
                    cursor,
                    direction.getOpposite()
            );
            if (dest == null) {
                continue;
            }
            budget -= moveEnergy(energy, dest, budget);
        }
    }

    private static int moveEnergy(EnergyHandler source, EnergyHandler dest, int want) {
        try (Transaction tx = Transaction.open(null)) {
            int canInsert;
            try (Transaction sim = Transaction.open(tx)) {
                canInsert = dest.insert(want, sim);
            }
            if (canInsert <= 0) {
                return 0;
            }
            int extracted = source.extract(canInsert, tx);
            if (extracted <= 0) {
                return 0;
            }
            int inserted = dest.insert(extracted, tx);
            if (inserted != extracted) {
                return 0;
            }
            tx.commit();
            return extracted;
        }
    }
}
