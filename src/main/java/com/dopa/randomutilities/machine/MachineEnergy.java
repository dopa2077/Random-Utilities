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
 * FE buffer for powered machines. Capacity and intake scale with energy upgrades
 * (configured percent of the default per upgrade, not compounding).
 * Speed and per-operation cost scale with stored FE / {@link #DEFAULT_CAPACITY}
 * once above a full default tank. Extra stored energy from upgrades goes faster
 * and costs more per cycle; installing upgrades does not slow a 100k charge.
 */
public final class MachineEnergy extends SimpleEnergyHandler {
    public static final int DEFAULT_CAPACITY = 100_000;
    public static final int DEFAULT_MAX_RECEIVE = 2_000;
    public static final int FE_PER_DISTANCE = 100;
    private static final int MAX_CYCLES_PER_TICK = 8;

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
        double bonus = UpgradeConfig.energyBonusPercent() / 100.0;
        return DEFAULT_CAPACITY + (int) Math.round(DEFAULT_CAPACITY * bonus * count);
    }

    public static int maxReceiveFor(int energyCount) {
        int count = Mth.clamp(energyCount, 0, UpgradeConfig.maxEnergy());
        double bonus = UpgradeConfig.energyBonusPercent() / 100.0;
        return DEFAULT_MAX_RECEIVE + (int) Math.round(DEFAULT_MAX_RECEIVE * bonus * count);
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

    public double speedFactor() {
        if (energy <= 0) {
            return 0.0;
        }
        return (double) energy / (double) DEFAULT_CAPACITY;
    }

    /**
     * Runs {@code operation} once per ready cycle this tick. Extra stored FE above the
     * default buffer can complete several cycles in one tick.
     */
    public void runReadyCycles(BooleanSupplier operation) {
        progress += speedFactor();
        int guard = 0;
        while (progress >= 1.0 && guard++ < MAX_CYCLES_PER_TICK) {
            if (operation.getAsBoolean()) {
                consumeCycle();
            } else {
                holdReadyCycle();
                return;
            }
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

    public int operationCost(BlockPos machine, BlockPos target, int efficiencyCount) {
        int distance = Math.max(1, WorkingVolume.chebyshev(machine, target));
        int base = FE_PER_DISTANCE * distance;
        int count = Mth.clamp(efficiencyCount, 0, UpgradeConfig.maxEfficiency());
        double remaining = 1.0 - (UpgradeConfig.efficiencyBonusPercent() / 100.0) * count;
        double overclock = Math.max(1.0, (double) energy / (double) DEFAULT_CAPACITY);
        return Math.max(1, (int) Math.round(base * remaining * overclock));
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
