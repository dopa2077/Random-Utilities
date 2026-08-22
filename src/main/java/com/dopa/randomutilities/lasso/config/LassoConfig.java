package com.dopa.randomutilities.lasso.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.lasso.LassoTier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Per-tier entity allow/deny lists from {@code config/dopas_random_utilities/items/lasso.json}. */
public final class LassoConfig {
    private static final String CONFIG_RELATIVE = "dopas_random_utilities/items/lasso.json";
    private static final String DEFAULT_RESOURCE = "/default/dopas_random_utilities/items/lasso.json";

    private static final Map<LassoTier, TierConfig> TIERS = new EnumMap<>(LassoTier.class);

    static {
        loadDefaultsFromJar();
    }

    private LassoConfig() {}

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
            dOPasRandomUtilities.LOGGER.error("Failed to load lasso config from {}, using defaults", configFile, exception);
            loadDefaultsFromJar();
        }
    }

    public static void reload() {
        load();
    }

    public static boolean allows(LassoTier tier, EntityType<?> type) {
        TierConfig config = TIERS.get(tier);
        if (config == null) {
            return true;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return false;
        }
        boolean listed = config.entities.contains(id);
        return config.mode == Mode.WHITELIST ? listed : !listed;
    }

    private static void loadDefaultsFromJar() {
        try (InputStream input = LassoConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                applyBuiltInDefaults();
                return;
            }
            applyJson(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled lasso config defaults", exception);
            applyBuiltInDefaults();
        }
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = LassoConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote lasso config at {}", configFile);
        }
    }

    private static void applyBuiltInDefaults() {
        TIERS.clear();
        TierConfig playerOnly = new TierConfig(Mode.BLACKLIST, Set.of(Identifier.withDefaultNamespace("player")));
        TIERS.put(LassoTier.BASIC, playerOnly);
        TIERS.put(LassoTier.GOLDEN, playerOnly);
        TIERS.put(LassoTier.CURSED, playerOnly);
    }

    private static void applyJson(JsonObject root) {
        applyBuiltInDefaults();
        putTier(root, "basic", LassoTier.BASIC);
        putTier(root, "golden", LassoTier.GOLDEN);
        putTier(root, "cursed", LassoTier.CURSED);
    }

    private static void putTier(JsonObject root, String key, LassoTier tier) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            return;
        }
        JsonObject section = root.getAsJsonObject(key);
        Mode mode = "whitelist".equalsIgnoreCase(section.get("mode").getAsString()) ? Mode.WHITELIST : Mode.BLACKLIST;
        Set<Identifier> entities = new HashSet<>();
        if (section.has("entities") && section.get("entities").isJsonArray()) {
            JsonArray array = section.getAsJsonArray("entities");
            for (JsonElement element : array) {
                entities.add(Identifier.parse(element.getAsString()));
            }
        }
        TIERS.put(tier, new TierConfig(mode, Set.copyOf(entities)));
    }

    private enum Mode {
        WHITELIST,
        BLACKLIST
    }

    private record TierConfig(Mode mode, Set<Identifier> entities) {}
}
