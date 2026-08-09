package com.dopa.randomutilities.fishnet.config;

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
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Custom fishnet treasure-mesh loot from
 * {@code config/dopas_random_utilities/items/treasure_loot.json}.
 * Weights are 0–1 (0 never, 1 always); picks one weighted entry per roll.
 */
public final class TreasureLootConfig {
    private static final String CONFIG_RELATIVE = "dopas_random_utilities/items/treasure_loot.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/treasure_loot.json";

    private static List<Entry> entries = List.of();

    static {
        loadDefaultsFromJar();
    }

    private TreasureLootConfig() {}

    public static void load() {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_RELATIVE);
        try {
            Files.createDirectories(configFile.getParent());
            if (Files.notExists(configFile)) {
                copyDefaultConfig(configFile);
            }
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                applyJson(JsonParser.parseReader(jsonReader).getAsJsonObject());
            }
            dOPasRandomUtilities.LOGGER.info(
                    "Loaded treasure loot config from {} ({} entries)",
                    configFile.toAbsolutePath(),
                    entries.size()
            );
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error(
                    "Failed to load treasure loot config from {}, using defaults",
                    configFile,
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

    /**
     * Weighted pick among entries with weight &gt; 0. Returns empty for air or empty table.
     */
    public static ItemStack roll(RandomSource random) {
        double total = 0.0;
        List<Entry> positive = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.weight() <= 0.0) {
                continue;
            }
            positive.add(entry);
            total += entry.weight();
        }
        if (positive.isEmpty() || total <= 0.0) {
            return ItemStack.EMPTY;
        }
        double pick = random.nextDouble() * total;
        double cursor = 0.0;
        for (Entry entry : positive) {
            cursor += entry.weight();
            if (pick <= cursor) {
                return entry.toStack();
            }
        }
        return positive.getLast().toStack();
    }

    private static void loadDefaultsFromJar() {
        try (InputStream input = TreasureLootConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled treasure loot defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = TreasureLootConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote treasure loot config at {}", configFile);
        }
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
        defaults.put("minecraft:echo_shard", 0.08);
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
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            Identifier id = parseItemId(entry.getKey());
            if (id == null) {
                dOPasRandomUtilities.LOGGER.warn("Invalid treasure loot item id '{}'", entry.getKey());
                continue;
            }
            double weight = Math.max(0.0, Math.min(1.0, entry.getValue()));
            built.add(new Entry(id, weight));
        }
        entries = Collections.unmodifiableList(built);
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
