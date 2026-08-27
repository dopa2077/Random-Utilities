package com.dopa.randomutilities.core.machine.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.machine.solar.furnace.config.SolarFurnaceConfig;
import com.dopa.randomutilities.logistics.transfer.HeadKind;
import com.dopa.randomutilities.logistics.transfer.config.TransferNodeConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Upgrade install caps from {@code upgrades/*.json}.
 * Bonuses: {@link UpgradeBonusesConfig}; baselines: blocks/ configs.
 */
public final class UpgradeConfig {
    public static final int UPGRADE_SLOT_COUNT = 6;
    /** Fortune Mesh never guarantees treasure; 9×10% default = 90%. */
    public static final int FORTUNE_MESH_MAX_CHANCE_PERCENT = 90;
    /** Treasure Mesh only switches loot tables; one is enough. */
    public static final int MAX_TREASURE_MESH = 1;
    /** Stack upgrades add this many items per installed upgrade (maximum {@link #MAX_STACK_UPGRADE}). */
    public static final int STACK_ITEMS_PER_UPGRADE = 8;
    /** Maximum stack upgrades installable on supported machines. */
    public static final int MAX_STACK_UPGRADE = 64;
    /** Per-upgrade multiplier used by fluid/energy transfer amount scaling. */
    public static final double FLUID_ENERGY_PI_BONUS = Math.PI;
    private static final String PI_BONUS_DISPLAY = "+3.14159...";

    private static int maxFortuneMeshFishnet = 9;
    private static int maxProductivityFishnet = 9;
    private static int maxOverclockFishnet = 15;
    private static int maxOverclockSolarFurnace = 10;
    private static int maxProductivitySolarFurnace = 7;
    private static int maxEnergy = 64;
    private static int maxEfficiency = 15;
    private static int maxRange = 16;
    private static int maxOverclockPowered = 11;
    private static int maxEnergyCombustion = 64;
    private static int maxEfficiencyCombustion = 15;
    private static int maxOverclockCombustion = 11;
    private static int maxEnergySolarPanel = 64;
    private static int maxEfficiencySolarPanel = 15;
    private static int maxOverclockSolarPanel = 11;
    private static int itemNodeMaxOverclock = 11;
    private static int fluidNodeMaxOverclock = 11;
    private static int maxFluidCapacity = 64;
    private static int energyNodeMaxOverclock = 11;
    private static int maxEnergyTransferNode = 64;
    private static final Map<GeneratorType, Integer> maxProductivityPerType = new EnumMap<>(GeneratorType.class);
    private static final Map<GeneratorType, Integer> maxOverclockPerType = new EnumMap<>(GeneratorType.class);

    private static final Map<String, Consumer<JsonObject>> CAP_LOADERS = new LinkedHashMap<>();

    static {
        CAP_LOADERS.put("upgrades/resource_generator.json", UpgradeConfig::applyGeneratorCaps);
        CAP_LOADERS.put("upgrades/solar_furnace.json", UpgradeConfig::applySolarCaps);
        CAP_LOADERS.put("upgrades/fishnet.json", UpgradeConfig::applyFishnetCaps);
        CAP_LOADERS.put("upgrades/transfer_node.json", UpgradeConfig::applyTransferCaps);
        CAP_LOADERS.put("upgrades/powered_machines.json", UpgradeConfig::applyPoweredCaps);
        CAP_LOADERS.put("upgrades/combustion_generator.json", UpgradeConfig::applyCombustionCaps);
        CAP_LOADERS.put("upgrades/solar_panel.json", UpgradeConfig::applySolarPanelCaps);
        applyBuiltInDefaults();
        loadDefaultsFromJar();
    }

    private UpgradeConfig() {}

    public static void load() {
        applyBuiltInDefaults();
        for (Map.Entry<String, Consumer<JsonObject>> entry : CAP_LOADERS.entrySet()) {
            String relative = entry.getKey();
            String jar = "/default/dopas_random_utilities/" + relative;
            ConfigPack.loadJson(relative, jar, entry.getValue(), () ->
                    dOPasRandomUtilities.LOGGER.warn("Using built-in defaults for {}", relative));
        }
    }

    public static void reload() {
        load();
    }

    public static int productivityBonusPercent() {
        return UpgradeBonusesConfig.productivity();
    }

    public static int overclockSpeedPercent() {
        return UpgradeBonusesConfig.overclock();
    }

    public static int fortuneMeshTreasurePercent() {
        return UpgradeBonusesConfig.treasure();
    }

    public static int efficiencyBonusPercent() {
        return UpgradeBonusesConfig.efficiency();
    }

    public static int rangeBonus() {
        return Math.max(0, UpgradeBonusesConfig.range());
    }

    public static int extraRange(int rangeCount) {
        return rangeBonus() * Math.max(0, rangeCount);
    }

    /** Treasure chance from installed Fortune Mesh, capped at {@link #FORTUNE_MESH_MAX_CHANCE_PERCENT}. */
    public static int fortuneMeshChancePercent(int meshCount) {
        int percent = fortuneMeshTreasurePercent() * Math.max(0, meshCount);
        if (percent <= 0) {
            return 0;
        }
        return Math.min(FORTUNE_MESH_MAX_CHANCE_PERCENT, percent);
    }

    public static int maxFortuneMeshFishnet() {
        int configured = Math.max(0, maxFortuneMeshFishnet);
        int per = Math.max(0, fortuneMeshTreasurePercent());
        if (per <= 0) {
            return configured;
        }
        return Math.min(configured, FORTUNE_MESH_MAX_CHANCE_PERCENT / per);
    }

    public static int maxTreasureMeshFishnet() {
        return MAX_TREASURE_MESH;
    }

    public static int maxProductivityFishnet() {
        return Math.max(0, maxProductivityFishnet);
    }

    public static int maxOverclockFishnet() {
        return Math.max(0, maxOverclockFishnet);
    }

    public static int maxOverclockSolarFurnace() {
        return Math.max(0, maxOverclockSolarFurnace);
    }

    public static int maxProductivitySolarFurnace() {
        return Math.max(0, maxProductivitySolarFurnace);
    }

    /** Noon cook speed vs a vanilla furnace: base peak plus the global overclock bonus per upgrade. */
    public static float solarPeakFactor(int overclockCount) {
        int count = Math.min(Math.max(0, overclockCount), maxOverclockSolarFurnace());
        return (SolarFurnaceConfig.peakSpeedPercent() + overclockSpeedPercent() * count) / 100.0F;
    }

    public static int solarPeakPercent(int overclockCount) {
        return Math.round(solarPeakFactor(overclockCount) * 100.0F);
    }

    public static int maxEnergy() {
        return Math.max(0, maxEnergy);
    }

    public static int maxEfficiency() {
        return Math.max(0, maxEfficiency);
    }

    public static int maxRange() {
        return Math.max(0, maxRange);
    }

    public static int maxOverclockPoweredMachines() {
        return Math.max(0, maxOverclockPowered);
    }

    public static int maxEnergyCombustion() {
        return Math.max(0, maxEnergyCombustion);
    }

    public static int maxEfficiencyCombustion() {
        return Math.max(0, maxEfficiencyCombustion);
    }

    public static int maxOverclockCombustion() {
        return Math.max(0, maxOverclockCombustion);
    }

    public static int maxEnergySolarPanel() {
        return Math.max(0, maxEnergySolarPanel);
    }

    public static int maxEfficiencySolarPanel() {
        return Math.max(0, maxEfficiencySolarPanel);
    }

    public static int maxOverclockSolarPanel() {
        return Math.max(0, maxOverclockSolarPanel);
    }

    /**
     * Softens dawn/dusk loss while leaving noon unchanged.
     * At {@link #maxEfficiencySolarPanel()} installed, negates 90% of the (1 − sun) tax.
     */
    public static float solarEfficiencyFactor(float sunFactor, int efficiencyCount) {
        float factor = Math.max(0.0F, Math.min(1.0F, sunFactor));
        int max = maxEfficiencySolarPanel();
        int count = Math.max(0, efficiencyCount);
        if (max <= 0 || count <= 0) {
            return factor;
        }
        float t = Math.min(1.0F, count / (float) max);
        float boost = 0.9F * t;
        return factor + (1.0F - factor) * boost;
    }

    public static int poweredBaseTicks() {
        return PoweredMachinesConfig.baseTicks();
    }

    public static double overclockCostExponent() {
        return PoweredMachinesConfig.overclockCostExponent();
    }

    public static int maxOverclockTransferNode(HeadKind kind) {
        return Math.max(0, switch (kind) {
            case ITEM -> itemNodeMaxOverclock;
            case FLUID -> fluidNodeMaxOverclock;
            case ENERGY -> energyNodeMaxOverclock;
        });
    }

    public static int maxFluidCapacity() {
        return Math.max(0, maxFluidCapacity);
    }

    public static int maxEnergyTransferNode() {
        return Math.max(0, maxEnergyTransferNode);
    }

    public static int capEnergyMachine(Item item) {
        if (item == ModItems.ENERGY_UPGRADE.get()) {
            return maxEnergy();
        }
        if (item == ModItems.EFFICIENCY_UPGRADE.get()) {
            return maxEfficiency();
        }
        if (item == ModItems.RANGE_UPGRADE.get()) {
            return maxRange();
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return maxOverclockPoweredMachines();
        }
        return 0;
    }

    public static int capCombustion(Item item) {
        if (item == ModItems.ENERGY_UPGRADE.get()) {
            return maxEnergyCombustion();
        }
        if (item == ModItems.EFFICIENCY_UPGRADE.get()) {
            return maxEfficiencyCombustion();
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return maxOverclockCombustion();
        }
        return 0;
    }

    public static int capSolarPanel(Item item) {
        if (item == ModItems.ENERGY_UPGRADE.get()) {
            return maxEnergySolarPanel();
        }
        if (item == ModItems.EFFICIENCY_UPGRADE.get()) {
            return maxEfficiencySolarPanel();
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return maxOverclockSolarPanel();
        }
        return 0;
    }

    public static int capSolarFurnace(Item item) {
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return maxOverclockSolarFurnace();
        }
        if (item == ModItems.PRODUCTIVITY_UPGRADE.get()) {
            return maxProductivitySolarFurnace();
        }
        return 0;
    }

    public static int capFishnet(Item item) {
        if (item == ModItems.PRODUCTIVITY_UPGRADE.get()) {
            return maxProductivityFishnet();
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return maxOverclockFishnet();
        }
        if (item == ModItems.FORTUNE_MESH_UPGRADE.get()) {
            return maxFortuneMeshFishnet();
        }
        if (item == ModItems.TREASURE_MESH_UPGRADE.get()) {
            return maxTreasureMeshFishnet();
        }
        return 0;
    }

    public static String piBonusDisplay() {
        return PI_BONUS_DISPLAY;
    }

    public static String piPercentTotal(int upgradeCount) {
        if (upgradeCount <= 0) {
            return "0%";
        }
        return String.format("+%.5f...", upgradeCount * FLUID_ENERGY_PI_BONUS) + "%";
    }

    public static int stackItemBonus(int stackCount) {
        int count = Math.min(Math.max(0, stackCount), MAX_STACK_UPGRADE);
        return count * STACK_ITEMS_PER_UPGRADE;
    }

    public static int stackTransferTotal(int stackCount) {
        return 1 + stackItemBonus(stackCount);
    }

    public static int stackCollectorTotal(int pickupBatch, int stackCount) {
        return Math.max(1, pickupBatch) + stackItemBonus(stackCount);
    }

    public static boolean stackAllowsMixedTypes(int stackCount) {
        return stackCount > 0;
    }

    public static int transferNodeInterval(HeadKind kind, int overclockCount) {
        int count = Math.min(Math.max(0, overclockCount), maxOverclockTransferNode(kind));
        return effectiveTicks(TransferNodeConfig.baseTicks(kind), count);
    }

    public static int transferNodeFluidAmount(int capacityCount) {
        int n = Math.min(Math.max(0, capacityCount), maxFluidCapacity());
        int base = TransferNodeConfig.baseMb();
        if (n <= 0) {
            return base;
        }
        return (int) Math.round(base * (1.0 + n * FLUID_ENERGY_PI_BONUS));
    }

    public static int transferNodeEnergyAmount(int energyCount) {
        int n = Math.min(Math.max(0, energyCount), maxEnergyTransferNode());
        int base = TransferNodeConfig.baseFe();
        if (n <= 0) {
            return base;
        }
        return (int) Math.round(base * (1.0 + n * FLUID_ENERGY_PI_BONUS));
    }

    public static int additivePercentOfBase(int base, int percentPer, int count, int maxCount) {
        if (base <= 0) {
            return 0;
        }
        int n = Math.min(Math.max(0, count), Math.max(0, maxCount));
        if (percentPer <= 0 || n <= 0) {
            return base;
        }
        return base + base * percentPer * n / 100;
    }

    public static int maxProductivity(GeneratorType type) {
        return Math.max(0, maxProductivityPerType.getOrDefault(type, 0));
    }

    public static int maxOverclock(GeneratorType type) {
        return Math.max(0, maxOverclockPerType.getOrDefault(type, 0));
    }

    public static boolean upgradesEnabled(GeneratorType type) {
        return maxProductivity(type) > 0 || maxOverclock(type) > 0;
    }

    public static int peekBoostedAmount(int baseAmount, int productivityCount, int bank) {
        if (baseAmount <= 0) {
            return baseAmount;
        }
        int bonus = productivityBonusPercent() * Math.max(0, productivityCount);
        if (bonus <= 0) {
            return baseAmount;
        }
        int safeBank = Math.max(0, bank);
        return baseAmount + (safeBank + baseAmount * bonus) / 100;
    }

    public static int advanceProductivityBank(int baseAmount, int productivityCount, int bank) {
        if (baseAmount <= 0) {
            return Math.max(0, bank);
        }
        int bonus = productivityBonusPercent() * Math.max(0, productivityCount);
        if (bonus <= 0) {
            return Math.max(0, bank);
        }
        return (Math.max(0, bank) + baseAmount * bonus) % 100;
    }

    public static float overclockSpeed(int overclockCount) {
        int count = Math.max(0, overclockCount);
        if (count <= 0) {
            return 1.0F;
        }
        return 1.0F + (overclockSpeedPercent() * count) / 100.0F;
    }

    public static int effectiveTicks(int recipeTicks, int overclockCount) {
        if (recipeTicks <= 0) {
            return 1;
        }
        int reduction = overclockSpeedPercent() * Math.max(0, overclockCount);
        if (reduction <= 0) {
            return recipeTicks;
        }
        if (reduction < 100) {
            return Math.max(1, recipeTicks * (100 - reduction) / 100);
        }
        return Math.max(1, (int) Math.round(recipeTicks / (double) reduction));
    }

    private static void loadDefaultsFromJar() {
        for (Map.Entry<String, Consumer<JsonObject>> entry : CAP_LOADERS.entrySet()) {
            String jar = "/default/dopas_random_utilities/" + entry.getKey();
            ConfigPack.loadJarJson(jar, entry.getValue(), () -> {});
        }
    }

    private static void applyBuiltInDefaults() {
        maxFortuneMeshFishnet = 9;
        maxProductivityFishnet = 9;
        maxOverclockFishnet = 15;
        maxOverclockSolarFurnace = 10;
        maxProductivitySolarFurnace = 7;
        maxEnergy = 64;
        maxEfficiency = 15;
        maxRange = 16;
        maxOverclockPowered = 11;
        maxEnergyCombustion = 64;
        maxEfficiencyCombustion = 64;
        maxOverclockCombustion = 384;
        maxEnergySolarPanel = 64;
        maxEfficiencySolarPanel = 64;
        maxOverclockSolarPanel = 0;
        itemNodeMaxOverclock = 11;
        fluidNodeMaxOverclock = 11;
        maxFluidCapacity = 64;
        energyNodeMaxOverclock = 11;
        maxEnergyTransferNode = 64;
        maxProductivityPerType.clear();
        maxOverclockPerType.clear();
        putGeneratorCaps(GeneratorType.BASIC_STONE, 5, 5);
        putGeneratorCaps(GeneratorType.INTERMEDIATE_STONE, 10, 10);
        putGeneratorCaps(GeneratorType.ADVANCED_STONE, 15, 15);
        putGeneratorCaps(GeneratorType.ELITE_STONE, 20, 20);
        putGeneratorCaps(GeneratorType.ULTIMATE_STONE, 32, 32);
        putGeneratorCaps(GeneratorType.RANDOM_ORE, 10, 10);
        putGeneratorCaps(GeneratorType.METAL_BLOCK, 15, 15);
        putGeneratorCaps(GeneratorType.CREATIVE_STONE, 0, 0);
        putGeneratorCaps(GeneratorType.CREATIVE_RANDOM_ORE, 0, 0);
        putGeneratorCaps(GeneratorType.CREATIVE_METAL_BLOCK, 0, 0);
    }

    private static void putGeneratorCaps(GeneratorType type, int maxOverclock, int maxProductivity) {
        maxOverclockPerType.put(type, maxOverclock);
        maxProductivityPerType.put(type, maxProductivity);
    }

    private static void applySolarCaps(JsonObject root) {
        maxOverclockSolarFurnace = ConfigPack.intOr(root, "max_overclock", maxOverclockSolarFurnace);
        maxProductivitySolarFurnace = ConfigPack.intOr(root, "max_productivity", maxProductivitySolarFurnace);
    }

    private static void applyFishnetCaps(JsonObject root) {
        maxProductivityFishnet = ConfigPack.intOr(root, "max_productivity", maxProductivityFishnet);
        maxOverclockFishnet = ConfigPack.intOr(root, "max_overclock", maxOverclockFishnet);
        maxFortuneMeshFishnet = ConfigPack.intOr(root, "max_treasure", maxFortuneMeshFishnet);
    }

    private static void applyPoweredCaps(JsonObject root) {
        maxEnergy = ConfigPack.intOr(root, "max_energy", maxEnergy);
        maxEfficiency = ConfigPack.intOr(root, "max_efficiency", maxEfficiency);
        maxRange = ConfigPack.intOr(root, "max_range", maxRange);
        maxOverclockPowered = ConfigPack.intOr(root, "max_overclock", maxOverclockPowered);
    }

    private static void applyCombustionCaps(JsonObject root) {
        maxEnergyCombustion = ConfigPack.intOr(root, "max_energy", maxEnergyCombustion);
        maxEfficiencyCombustion = ConfigPack.intOr(root, "max_efficiency", maxEfficiencyCombustion);
        maxOverclockCombustion = ConfigPack.intOr(root, "max_overclock", maxOverclockCombustion);
    }

    private static void applySolarPanelCaps(JsonObject root) {
        maxEnergySolarPanel = ConfigPack.intOr(root, "max_energy", maxEnergySolarPanel);
        maxEfficiencySolarPanel = ConfigPack.intOr(root, "max_efficiency", maxEfficiencySolarPanel);
        maxOverclockSolarPanel = ConfigPack.intOr(root, "max_overclock", maxOverclockSolarPanel);
    }

    private static void applyGeneratorCaps(JsonObject generators) {
        for (GeneratorType type : GeneratorType.values()) {
            JsonElement element = generators.get(type.id());
            if (element == null) {
                continue;
            }
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                maxProductivityPerType.put(type, ConfigPack.intOr(object, "max_productivity", maxProductivity(type)));
                maxOverclockPerType.put(type, ConfigPack.intOr(object, "max_overclock", maxOverclock(type)));
            } else if (element.isJsonPrimitive()) {
                int cap = Math.max(0, element.getAsInt());
                putGeneratorCaps(type, cap, cap);
            }
        }
    }

    private static void applyTransferCaps(JsonObject root) {
        JsonObject item = root.getAsJsonObject("item");
        if (item != null) {
            itemNodeMaxOverclock = ConfigPack.intOr(item, "max_overclock", itemNodeMaxOverclock);
        }
        JsonObject fluid = root.getAsJsonObject("fluid");
        if (fluid != null) {
            fluidNodeMaxOverclock = ConfigPack.intOr(fluid, "max_overclock", fluidNodeMaxOverclock);
            maxFluidCapacity = ConfigPack.intOr(fluid, "max_fluid_capacity", maxFluidCapacity);
        }
        JsonObject energy = root.getAsJsonObject("energy");
        if (energy != null) {
            energyNodeMaxOverclock = ConfigPack.intOr(energy, "max_overclock", energyNodeMaxOverclock);
            maxEnergyTransferNode = ConfigPack.intOr(energy, "max_energy", maxEnergyTransferNode);
        }
    }
}
