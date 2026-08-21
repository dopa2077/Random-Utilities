package com.dopa.randomutilities.machine;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.util.WorkingVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.BooleanSupplier;

/**
 * FE buffer for powered machines. Capacity and intake grow by one default chunk
 * per energy upgrade. Cycle speed comes from overclock upgrades (base ticks from
 * config); operation cost scales with distance, {@code (baseTicks/ticks)^exponent},
 * and efficiency — not with stored FE.
 */
public final class MachineEnergy extends SimpleEnergyHandler {
    public static final int DEFAULT_CAPACITY = 20_000;
    public static final int DEFAULT_MAX_RECEIVE = 500;
    public static final int FE_PER_DISTANCE = 100;

    private double progress;
    private int lastTickUsage;

    public MachineEnergy() {
        super(DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_CAPACITY, 0);
    }

    public interface Host {
        MachineEnergy energy();
    }

    public static int capacityFor(int energyCount) {
        int count = Mth.clamp(energyCount, 0, UpgradeConfig.maxEnergy());
        return DEFAULT_CAPACITY * (1 + count);
    }

    public static int maxReceiveFor(int energyCount) {
        int count = Mth.clamp(energyCount, 0, UpgradeConfig.maxEnergy());
        return DEFAULT_MAX_RECEIVE * (1 + count);
    }

    public static boolean wouldVoidEnergy(int stored, int energyCountAfter) {
        return stored > capacityFor(energyCountAfter);
    }

    public void applyEnergyUpgrades(int energyCount) {
        capacity = capacityFor(energyCount);
        maxInsert = maxReceiveFor(energyCount);
        maxExtract = capacity;
        clampToCapacity();
    }

    private void clampToCapacity() {
        if (energy > capacity) {
            energy = capacity;
        }
    }

    public int stored() {
        return energy;
    }

    public int capacity() {
        return capacity;
    }

    public int maxReceive() {
        return maxInsert;
    }

    public int lastTickUsage() {
        return lastTickUsage;
    }

    public void beginTick() {
        lastTickUsage = 0;
    }

    /**
     * Advances one cycle interval this tick and runs {@code operation} at most once
     * when progress reaches a full cycle.
     */
    public void runReadyCycles(int overclockCount, BooleanSupplier operation) {
        int interval = UpgradeConfig.effectiveTicks(
                UpgradeConfig.poweredBaseTicks(),
                Mth.clamp(overclockCount, 0, UpgradeConfig.maxOverclockPoweredMachines())
        );
        progress += 1.0 / (double) interval;
        if (progress < 1.0) {
            return;
        }
        if (operation.getAsBoolean()) {
            consumeCycle();
        } else {
            holdReadyCycle();
        }
    }

    public void consumeCycle() {
        progress -= 1.0;
        if (progress < 0.0) {
            progress = 0.0;
        }
    }

    /** Keep a ready cycle while waiting for a target or enough FE. */
    public void holdReadyCycle() {
        if (progress > 1.0) {
            progress = 1.0;
        }
    }

    public int operationCost(BlockPos machine, BlockPos target, int efficiencyCount, int overclockCount) {
        int distance = Math.max(1, WorkingVolume.chebyshev(machine, target));
        int base = FE_PER_DISTANCE * distance;
        int baseTicks = UpgradeConfig.poweredBaseTicks();
        int ticks = UpgradeConfig.effectiveTicks(
                baseTicks,
                Mth.clamp(overclockCount, 0, UpgradeConfig.maxOverclockPoweredMachines())
        );
        double speedFactor = (double) baseTicks / (double) ticks;
        double tax = Math.pow(speedFactor, UpgradeConfig.overclockCostExponent());
        int effCount = Mth.clamp(efficiencyCount, 0, UpgradeConfig.maxEfficiency());
        double remaining = 1.0 - (UpgradeConfig.efficiencyBonusPercent() / 100.0) * effCount;
        remaining = Math.max(0.0, remaining);
        return Math.max(1, (int) Math.round(base * tax * remaining));
    }

    public boolean tryConsume(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (energy < amount) {
            return false;
        }
        try (Transaction tx = Transaction.open(null)) {
            int extracted = extract(amount, tx);
            if (extracted >= amount) {
                tx.commit();
                lastTickUsage += extracted;
                return true;
            }
        }
        return false;
    }

    public void save(ValueOutput output) {
        serialize(output);
        if (progress != 0.0) {
            output.putDouble("EnergyProgress", progress);
        }
    }

    public void load(ValueInput input) {
        deserialize(input);
        progress = Math.max(0.0, input.getDoubleOr("EnergyProgress", 0.0));
        clampToCapacity();
    }
}
