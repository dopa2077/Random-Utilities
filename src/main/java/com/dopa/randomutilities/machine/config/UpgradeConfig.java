package com.dopa.randomutilities.machine.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.generator.config.GeneratorType;
import com.dopa.randomutilities.transfer.HeadKind;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;

/**
 * Upgrade percents and per-machine caps from
 * {@code config/dopas_random_utilities/items/upgrade.json}.
 * Missing config is copied from the bundled default; an existing file is never overwritten.
 */
public final class UpgradeConfig {
    public static final int UPGRADE_SLOT_COUNT = 6;
    /** Fortune Mesh never guarantees treasure; 9×10% default = 90%. */
    public static final int FORTUNE_MESH_MAX_CHANCE_PERCENT = 90;
    /** Treasure Mesh only switches loot tables; one is enough. */
    public static final int MAX_TREASURE_MESH = 1;
    /** Stack upgrade moves up to 64 items per transfer; one is enough. */
    public static final int MAX_STACK_UPGRADE = 1;

    private static final String CONFIG_RELATIVE = "dopas_random_utilities/items/upgrade.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/upgrade.json";

    private static int productivityBonusPercent = 13;
    private static int overclockSpeedPercent = 9;
    private static int fortuneMeshTreasurePercent = 10;
    private static int energyBonusPercent = 25;
    private static int efficiencyBonusPercent = 6;
    private static int rangeBonus = 1;
    private static int maxFortuneMeshFishnet = 9;
    private static int maxProductivityFishnet = 9;
    private static int maxOverclockFishnet = 15;
    private static int maxOverclockSolarFurnace = 10;
    private static int solarPeakSpeedPercent = 70;
    private static int maxEnergy = 64;
    private static int maxEfficiency = 15;
    private static int maxRange = 16;
    private static int maxOverclockPowered = 11;
    private static int poweredBaseTicks = 40;
    private static double overclockCostExponent = 1.09;
    private static int fluidCapacityBonusPercent = 10;
    private static int itemNodeBaseTicks = 40;
    private static int itemNodeMaxOverclock = 11;
    private static int fluidNodeBaseTicks = 60;
    private static int fluidNodeBaseMb = 100;
    private static int fluidNodeMaxOverclock = 11;
    private static int maxFluidCapacity = 64;
    private static int energyNodeBaseTicks = 80;
    private static int energyNodeBaseFe = 200;
    private static int energyNodeMaxOverclock = 11;
    private static int maxEnergyTransferNode = 64;
    private static final Map<GeneratorType, Integer> maxProductivityPerType = new EnumMap<>(GeneratorType.class);
    private static final Map<GeneratorType, Integer> maxOverclockPerType = new EnumMap<>(GeneratorType.class);

    static {
        applyBuiltInDefaults();
        loadDefaultsFromJar();
    }

    private UpgradeConfig() {}

    public static void load() {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_RELATIVE);
        try {
            Files.createDirectories(configFile.getParent());
            if (Files.notExists(configFile)) {
                copyDefaultConfig(configFile);
            }
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                applyJson(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load upgrade config from {}, using defaults", configFile, exception);
            loadDefaultsFromJar();
        }
    }

    public static void reload() {
        load();
    }

    public static int productivityBonusPercent() {
        return productivityBonusPercent;
    }

    public static int overclockSpeedPercent() {
        return overclockSpeedPercent;
    }

    public static int fortuneMeshTreasurePercent() {
        return fortuneMeshTreasurePercent;
    }

    public static int energyBonusPercent() {
        return energyBonusPercent;
    }

    public static int efficiencyBonusPercent() {
        return efficiencyBonusPercent;
    }

    public static int rangeBonus() {
        return Math.max(0, rangeBonus);
    }

    public static int extraRange(int rangeCount) {
        return rangeBonus() * Math.max(0, rangeCount);
    }

    /** Treasure chance from installed Fortune Mesh, capped at {@link #FORTUNE_MESH_MAX_CHANCE_PERCENT}. */
    public static int fortuneMeshChancePercent(int meshCount) {
        int percent = fortuneMeshTreasurePercent * Math.max(0, meshCount);
        if (percent <= 0) {
            return 0;
        }
        return Math.min(FORTUNE_MESH_MAX_CHANCE_PERCENT, percent);
    }

    public static int maxFortuneMeshFishnet() {
        int configured = Math.max(0, maxFortuneMeshFishnet);
        int per = Math.max(0, fortuneMeshTreasurePercent);
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

    /** Noon cook speed vs a vanilla furnace: base peak plus the global overclock bonus per upgrade. */
    public static float solarPeakFactor(int overclockCount) {
        int count = Math.min(Math.max(0, overclockCount), maxOverclockSolarFurnace());
        return (solarPeakSpeedPercent + overclockSpeedPercent * count) / 100.0F;
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

    public static int poweredBaseTicks() {
        return Math.max(1, poweredBaseTicks);
    }

    public static double overclockCostExponent() {
        return Math.max(0.0, overclockCostExponent);
    }

    public static int fluidCapacityBonusPercent() {
        return fluidCapacityBonusPercent;
    }

    public static int maxOverclockTransferNode() {
        return maxOverclockTransferNode(HeadKind.ITEM);
    }

    public static int maxOverclockTransferNode(HeadKind kind) {
        return Math.max(0, switch (kind) {
            case ITEM -> itemNodeMaxOverclock;
            case FLUID -> fluidNodeMaxOverclock;
            case ENERGY -> energyNodeMaxOverclock;
        });
    }

    public static int transferNodeBaseTicks() {
        return transferNodeBaseTicks(HeadKind.ITEM);
    }

    public static int transferNodeBaseTicks(HeadKind kind) {
        return Math.max(1, switch (kind) {
            case ITEM -> itemNodeBaseTicks;
            case FLUID -> fluidNodeBaseTicks;
            case ENERGY -> energyNodeBaseTicks;
        });
    }

    public static int transferNodeBaseMb() {
        return Math.max(0, fluidNodeBaseMb);
    }

    public static int transferNodeBaseFe() {
        return Math.max(0, energyNodeBaseFe);
    }

    public static int maxFluidCapacity() {
        return Math.max(0, maxFluidCapacity);
    }

    public static int maxEnergyTransferNode() {
        return Math.max(0, maxEnergyTransferNode);
    }

    public static int transferNodeInterval(int overclockCount) {
        return transferNodeInterval(HeadKind.ITEM, overclockCount);
    }

    public static int transferNodeInterval(HeadKind kind, int overclockCount) {
        int count = Math.min(Math.max(0, overclockCount), maxOverclockTransferNode(kind));
        return effectiveTicks(transferNodeBaseTicks(kind), count);
    }

    public static int transferNodeFluidAmount(int capacityCount) {
        return additivePercentOfBase(transferNodeBaseMb(), fluidCapacityBonusPercent(), capacityCount, maxFluidCapacity());
    }

    public static int transferNodeEnergyAmount(int energyCount) {
        int n = Math.min(Math.max(0, energyCount), maxEnergyTransferNode());
        return transferNodeBaseFe() * (1 + n);
    }

    /** 10% of base per upgrade, not compounding. */
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

    /**
     * Peek produce amount for the current productivity bank without mutating it.
     * Uses fractional carry: {@code bank + base * bonusPercent} percent-units.
     */
    public static int peekBoostedAmount(int baseAmount, int productivityCount, int bank) {
        if (baseAmount <= 0) {
            return baseAmount;
        }
        int bonus = productivityBonusPercent * Math.max(0, productivityCount);
        if (bonus <= 0) {
            return baseAmount;
        }
        int safeBank = Math.max(0, bank);
        return baseAmount + (safeBank + baseAmount * bonus) / 100;
    }

    /**
     * Advance the productivity remainder bank after a successful craft.
     * Returns the new bank (0–99 leftover percent-units typically, can be larger before mod).
     */
    public static int advanceProductivityBank(int baseAmount, int productivityCount, int bank) {
        if (baseAmount <= 0) {
            return Math.max(0, bank);
        }
        int bonus = productivityBonusPercent * Math.max(0, productivityCount);
        if (bonus <= 0) {
            return Math.max(0, bank);
        }
        return (Math.max(0, bank) + baseAmount * bonus) % 100;
    }

    /**
     * Additive craft-speed multiplier for resource generators.
     * Base is 1×; each overclock adds {@link #overclockSpeedPercent()}% of base
     * (32 × 9% → 1 + 2.88 = 3.88×).
     */
    public static float overclockSpeed(int overclockCount) {
        int count = Math.max(0, overclockCount);
        if (count <= 0) {
            return 1.0F;
        }
        return 1.0F + (overclockSpeedPercent * count) / 100.0F;
    }

    public static int effectiveTicks(int recipeTicks, int overclockCount) {
        if (recipeTicks <= 0) {
            return 1;
        }
        int reduction = overclockSpeedPercent * Math.max(0, overclockCount);
        if (reduction <= 0) {
            return recipeTicks;
        }
        if (reduction < 100) {
            return Math.max(1, recipeTicks * (100 - reduction) / 100);
        }
        return Math.max(1, (int) Math.round(recipeTicks / (double) reduction));
    }

    private static void loadDefaultsFromJar() {
        try (InputStream input = UpgradeConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled upgrade config defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = UpgradeConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote upgrade config at {}", configFile);
        }
    }

    private static void applyBuiltInDefaults() {
        productivityBonusPercent = 13;
        overclockSpeedPercent = 9;
        fortuneMeshTreasurePercent = 10;
        energyBonusPercent = 25;
        efficiencyBonusPercent = 6;
        rangeBonus = 1;
        maxFortuneMeshFishnet = 9;
        maxProductivityFishnet = 9;
        maxOverclockFishnet = 15;
        maxOverclockSolarFurnace = 10;
        solarPeakSpeedPercent = 70;
        maxEnergy = 64;
        maxEfficiency = 15;
        maxRange = 16;
        maxOverclockPowered = 11;
        poweredBaseTicks = 40;
        overclockCostExponent = 1.09;
        fluidCapacityBonusPercent = 10;
        itemNodeBaseTicks = 40;
        itemNodeMaxOverclock = 11;
        fluidNodeBaseTicks = 60;
        fluidNodeBaseMb = 100;
        fluidNodeMaxOverclock = 11;
        maxFluidCapacity = 64;
        energyNodeBaseTicks = 80;
        energyNodeBaseFe = 200;
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

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        applyBonuses(root);
        applyGeneratorCaps(root);
        applySolarFurnace(root);
        applyFishnet(root);
        applyPoweredMachines(root);
        applyTransferNode(root);
    }

    private static void applyBonuses(JsonObject root) {
        JsonObject bonuses = root.getAsJsonObject("bonuses_percent_per_upgrade");
        if (bonuses == null) {
            bonuses = root.getAsJsonObject("bonuses");
        }
        if (bonuses == null) {
            return;
        }
        productivityBonusPercent = intOrKeys(bonuses, productivityBonusPercent, "productivity", "productivity_percent_per_upgrade");
        overclockSpeedPercent = intOrKeys(bonuses, overclockSpeedPercent, "overclock", "overclock_speed_percent_per_upgrade");
        fortuneMeshTreasurePercent = intOrKeys(bonuses, fortuneMeshTreasurePercent, "treasure", "treasure_percent_per_upgrade");
        energyBonusPercent = intOr(bonuses, "energy", energyBonusPercent);
        efficiencyBonusPercent = intOr(bonuses, "efficiency", efficiencyBonusPercent);
        rangeBonus = intOr(bonuses, "range", rangeBonus);
        fluidCapacityBonusPercent = intOr(bonuses, "fluid_capacity", fluidCapacityBonusPercent);
    }

    private static void applyGeneratorCaps(JsonObject root) {
        JsonObject generators = root.getAsJsonObject("generators");
        if (generators == null) {
            return;
        }
        for (GeneratorType type : GeneratorType.values()) {
            JsonElement element = generators.get(type.id());
            if (element == null) {
                continue;
            }
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("max_productivity") || object.has("max_overclock")) {
                    maxProductivityPerType.put(type, intOr(object, "max_productivity", maxProductivity(type)));
                    maxOverclockPerType.put(type, intOr(object, "max_overclock", maxOverclock(type)));
                } else if (object.has("max_upgrades")) {
                    int cap = intOr(object, "max_upgrades", 0);
                    putGeneratorCaps(type, cap, cap);
                }
            } else if (element.isJsonPrimitive()) {
                int cap = Math.max(0, element.getAsInt());
                putGeneratorCaps(type, cap, cap);
            }
        }
    }

    private static void applySolarFurnace(JsonObject root) {
        JsonObject solar = root.getAsJsonObject("solar_furnace");
        if (solar != null) {
            maxOverclockSolarFurnace = intOr(solar, "max_overclock", maxOverclockSolarFurnace);
            solarPeakSpeedPercent = intOr(solar, "peak_speed_percent", solarPeakSpeedPercent);
        }
    }

    private static void applyPoweredMachines(JsonObject root) {
        JsonObject powered = root.getAsJsonObject("powered_machines");
        if (powered != null) {
            maxEnergy = intOr(powered, "max_energy", maxEnergy);
            maxEfficiency = intOr(powered, "max_efficiency", maxEfficiency);
            maxRange = intOr(powered, "max_range", maxRange);
            maxOverclockPowered = intOr(powered, "max_overclock", maxOverclockPowered);
            poweredBaseTicks = Math.max(1, intOr(powered, "base_ticks", poweredBaseTicks));
            overclockCostExponent = doubleOr(powered, "overclock_cost_exponent", overclockCostExponent);
        }
    }

    private static void applyTransferNode(JsonObject root) {
        JsonObject legacy = root.getAsJsonObject("transfer_node");
        if (legacy != null) {
            int ticks = Math.max(1, intOr(legacy, "base_ticks", itemNodeBaseTicks));
            int overclock = intOr(legacy, "max_overclock", itemNodeMaxOverclock);
            itemNodeBaseTicks = ticks;
            fluidNodeBaseTicks = ticks;
            energyNodeBaseTicks = ticks;
            itemNodeMaxOverclock = overclock;
            fluidNodeMaxOverclock = overclock;
            energyNodeMaxOverclock = overclock;
        }
        JsonObject item = root.getAsJsonObject("transfer_node_item");
        if (item != null) {
            itemNodeBaseTicks = Math.max(1, intOr(item, "base_ticks", itemNodeBaseTicks));
            itemNodeMaxOverclock = intOr(item, "max_overclock", itemNodeMaxOverclock);
        }
        JsonObject fluid = root.getAsJsonObject("transfer_node_fluid");
        if (fluid != null) {
            fluidNodeBaseTicks = Math.max(1, intOr(fluid, "base_ticks", fluidNodeBaseTicks));
            fluidNodeBaseMb = intOr(fluid, "base_mb", fluidNodeBaseMb);
            fluidNodeMaxOverclock = intOr(fluid, "max_overclock", fluidNodeMaxOverclock);
            maxFluidCapacity = intOr(fluid, "max_fluid_capacity", maxFluidCapacity);
        }
        JsonObject energy = root.getAsJsonObject("transfer_node_energy");
        if (energy != null) {
            energyNodeBaseTicks = Math.max(1, intOr(energy, "base_ticks", energyNodeBaseTicks));
            energyNodeBaseFe = intOr(energy, "base_fe", energyNodeBaseFe);
            energyNodeMaxOverclock = intOr(energy, "max_overclock", energyNodeMaxOverclock);
            maxEnergyTransferNode = intOr(energy, "max_energy", maxEnergyTransferNode);
        }
    }

    private static void applyFishnet(JsonObject root) {
        JsonObject fishnet = root.getAsJsonObject("fishnet");
        if (fishnet == null) {
            return;
        }
        maxProductivityFishnet = intOr(fishnet, "max_productivity", maxProductivityFishnet);
        maxOverclockFishnet = intOr(fishnet, "max_overclock", maxOverclockFishnet);
        maxFortuneMeshFishnet = intOrKeys(fishnet, maxFortuneMeshFishnet, "max_treasure");
        JsonObject fortune = fishnet.getAsJsonObject("fortune_mesh");
        if (fortune != null) {
            if (!fishnet.has("max_treasure")) {
                maxFortuneMeshFishnet = intOr(fortune, "max", maxFortuneMeshFishnet);
            }
            fortuneMeshTreasurePercent = intOr(fortune, "treasure_percent_per_upgrade", fortuneMeshTreasurePercent);
        }
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0, object.get(key).getAsInt());
    }

    private static double doubleOr(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0.0, object.get(key).getAsDouble());
    }

    private static int intOrKeys(JsonObject object, int fallback, String... keys) {
        if (object == null) {
            return fallback;
        }
        for (String key : keys) {
            if (object.has(key) && object.get(key).isJsonPrimitive()) {
                return Math.max(0, object.get(key).getAsInt());
            }
        }
        return fallback;
    }
}
