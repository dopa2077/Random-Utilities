package com.dopa.randomutilities.machine.fishnet.config;

import com.dopa.randomutilities.config.ConfigPack;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Treasure Mesh loot table ({@code treasure/treasure_loot.json}).
 * Weights are relative among entries with weight &gt; 0.
 */
public final class TreasureLootConfig {
    private static final String RELATIVE = "treasure/treasure_loot.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/treasure/treasure_loot.json";

    private static List<Entry> entries = List.of();
    private static List<Entry> positiveEntries = List.of();
    private static double positiveTotalWeight;

    static {
        loadDefaultsFromJar();
    }

    private TreasureLootConfig() {}

    public static void load() {
        try {
            Path configFile = ConfigPack.ensureFile(RELATIVE, DEFAULT_RESOURCE);
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                applyJson(JsonParser.parseReader(jsonReader).getAsJsonObject());
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error(
                    "Failed to load treasure loot config, using defaults",
                    exception
            );
            loadDefaultsFromJar();
        }
    }

    public static void reload() {
        load();
    }

    /** Snapshot of configured entries (id + weight), including air / weight 0. */
    public static List<Entry> entries() {
        return entries;
    }

    /** Weighted pick among entries with weight &gt; 0. Returns empty for air or empty table. */
    public static ItemStack roll(RandomSource random) {
        if (positiveEntries.isEmpty() || positiveTotalWeight <= 0.0) {
            return ItemStack.EMPTY;
        }
        double pick = random.nextDouble() * positiveTotalWeight;
        double cursor = 0.0;
        for (Entry entry : positiveEntries) {
            cursor += entry.weight();
            if (pick <= cursor) {
                return entry.toStack();
            }
        }
        return positiveEntries.getLast().toStack();
    }

    private static void loadDefaultsFromJar() {
        ConfigPack.loadJarJson(DEFAULT_RESOURCE, TreasureLootConfig::applyJson, TreasureLootConfig::applyBuiltInDefaults);
    }

    private static void applyBuiltInDefaults() {
        Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("minecraft:air", 0.35);
        defaults.put("minecraft:heart_of_the_sea", 0.015);
        defaults.put("minecraft:conduit", 0.025);
        defaults.put("minecraft:nautilus_shell", 0.25);
        defaults.put("minecraft:prismarine_crystals", 0.4);
        defaults.put("minecraft:prismarine_shard", 0.4);
        defaults.put("minecraft:nether_star", 0.001);
        defaults.put("minecraft:echo_shard", 0.07);
        defaults.put("minecraft:diamond", 0.1);
        defaults.put("minecraft:emerald", 0.09);
        applyMap(defaults);
    }

    private static void applyJson(JsonObject root) {
        Map<String, Double> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                dOPasRandomUtilities.LOGGER.warn("Ignoring invalid treasure loot entry '{}'", entry.getKey());
                continue;
            }
            parsed.put(entry.getKey(), entry.getValue().getAsDouble());
        }
        if (parsed.isEmpty()) {
            applyBuiltInDefaults();
            return;
        }
        applyMap(parsed);
    }

    private static void applyMap(Map<String, Double> raw) {
        List<Entry> built = new ArrayList<>();
        List<Entry> positive = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            Identifier id = parseItemId(entry.getKey());
            if (id == null) {
                dOPasRandomUtilities.LOGGER.warn("Invalid treasure loot item id '{}'", entry.getKey());
                continue;
            }
            double weight = Math.max(0.0, Math.min(1.0, entry.getValue()));
            Entry builtEntry = new Entry(id, weight);
            built.add(builtEntry);
            if (weight > 0.0) {
                positive.add(builtEntry);
                total += weight;
            }
        }
        entries = Collections.unmodifiableList(built);
        positiveEntries = Collections.unmodifiableList(positive);
        positiveTotalWeight = total;
    }

    private static Identifier parseItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.contains(":") ? raw : "minecraft:" + raw;
        return Identifier.tryParse(value);
    }

    public record Entry(Identifier id, double weight) {
        public boolean isAir() {
            return id.equals(Identifier.withDefaultNamespace("air"));
        }

        public ItemStack toStack() {
            if (isAir() || weight <= 0.0) {
                return ItemStack.EMPTY;
            }
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isEmpty() || item.get() == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item.get());
        }

        /** Localized item name for JEI (falls back to the id if unknown). */
        public Component displayName() {
            if (isAir()) {
                return Items.AIR.getName(ItemStack.EMPTY);
            }
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isEmpty()) {
                return Component.literal(id.toString());
            }
            ItemStack stack = new ItemStack(item.get());
            return item.get().getName(stack);
        }
    }
}
