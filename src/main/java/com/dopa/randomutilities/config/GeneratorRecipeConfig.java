package com.dopa.randomutilities.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GeneratorRecipeConfig {
    private static final Map<GeneratorType, List<GeneratorRecipe>> RECIPE_MAP = new EnumMap<>(GeneratorType.class);

    private GeneratorRecipeConfig() {}

    public static void load() {
        RECIPE_MAP.clear();

        for (GeneratorType type : GeneratorType.values()) {
            Path configFile = FMLPaths.CONFIGDIR.get().resolve(type.configRelativePath());
            try {
                Files.createDirectories(configFile.getParent());
                if (Files.notExists(configFile)) {
                    copyDefaultConfig(type, configFile);
                }

                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    parseAndApply(type, reader);
                }
            } catch (IOException exception) {
                dOPasRandomUtilities.LOGGER.error(
                        "Failed to load generator config from {}",
                        configFile,
                        exception
                );
                loadDefaultFromJar(type);
            }
        }
    }

    public static void reload() {
        load();
    }

    public static List<GeneratorRecipe> getRecipes(GeneratorType type) {
        return RECIPE_MAP.getOrDefault(type, List.of());
    }

    private static void copyDefaultConfig(GeneratorType type, Path configFile) throws IOException {
        try (InputStream input = GeneratorRecipeConfig.class.getResourceAsStream(type.defaultResourcePath())) {
            if (input == null) {
                throw new IOException("Missing bundled default config at " + type.defaultResourcePath());
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote generator config at {}", configFile);
        }
    }

    private static void loadDefaultFromJar(GeneratorType type) {
        try (InputStream input = GeneratorRecipeConfig.class.getResourceAsStream(type.defaultResourcePath())) {
            if (input == null) {
                applyFallback(type);
                return;
            }
            parseAndApply(type, new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            dOPasRandomUtilities.LOGGER.error(
                    "Failed to load bundled default config for {}",
                    type.id(),
                    exception
            );
            applyFallback(type);
        }
    }

    private static void applyFallback(GeneratorType type) {
        switch (type.mode()) {
            case RECIPE -> RECIPE_MAP.put(type, List.of(createFallbackCobblestoneRecipe()));
            case RANDOM_ORE, METAL_BLOCK -> RECIPE_MAP.put(type, List.of(createFallbackRandomRecipe(type)));
        }
    }

    private static void parseAndApply(GeneratorType type, Reader reader) {
        JsonElement root;
        try {
            root = JsonParser.parseReader(reader);
        } catch (JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Invalid generator JSON syntax for {}", type.id(), exception);
            applyFallback(type);
            return;
        }

        switch (type.mode()) {
            case RECIPE -> parseRecipeFile(type, root, false);
            case RANDOM_ORE, METAL_BLOCK -> {
                // Prefer the shared recipes[] format; keep a tiny legacy fallback for old configs.
                if (root.isJsonObject() && root.getAsJsonObject().has("recipes")) {
                    parseRecipeFile(type, root, true);
                } else {
                    parseLegacyRandomFile(type, root);
                }
            }
        }
    }

    private static void parseLegacyRandomFile(GeneratorType type, JsonElement root) {
        if (!root.isJsonObject()) {
            dOPasRandomUtilities.LOGGER.error("Expected object/recipes config for {}", type.id());
            applyFallback(type);
            return;
        }

        JsonObject object = root.getAsJsonObject();
        int ticks = object.has("ticks") ? object.get("ticks").getAsInt() : 40;
        int amount = object.has("amount") ? object.get("amount").getAsInt() : 1;
        GeneratorOutputMode outputMode = object.has("output")
                ? GeneratorOutputMode.parse(object.get("output").getAsString())
                : GeneratorOutputMode.PLACE;

        if (ticks <= 0) {
            ticks = 40;
        }
        if (amount <= 0) {
            amount = 1;
        }

        GeneratorRecipe recipe = new GeneratorRecipe(
                "default",
                null,
                null,
                Arrays.asList(null, null, null, null),
                new boolean[] {false, false, false, false},
                null,
                ticks,
                amount,
                outputMode
        );
        RECIPE_MAP.put(type, List.of(recipe));
        dOPasRandomUtilities.LOGGER.info(
                "Loaded legacy {} settings as a single recipe: ticks={}, amount={}, output={}",
                type.id(),
                recipe.ticks(),
                recipe.amount(),
                recipe.outputMode().id()
        );
    }

    private static void parseRecipeFile(GeneratorType type, JsonElement root, boolean allowRandomResult) {
        JsonArray recipeArray;
        GeneratorOutputMode fileDefaultOutput = allowRandomResult
                ? GeneratorOutputMode.PLACE
                : GeneratorOutputMode.INSERT;

        try {
            if (root.isJsonArray()) {
                recipeArray = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                if (object.has("output")) {
                    fileDefaultOutput = GeneratorOutputMode.parse(object.get("output").getAsString());
                }
                JsonElement recipesElement = object.get("recipes");
                if (recipesElement == null || !recipesElement.isJsonArray()) {
                    throw new JsonSyntaxException("Expected a 'recipes' array");
                }
                recipeArray = recipesElement.getAsJsonArray();
            } else {
                throw new JsonSyntaxException("Expected recipe object or array");
            }
        } catch (JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Invalid generator recipe JSON for {}", type.id(), exception);
            applyFallback(type);
            return;
        }

        if (recipeArray.isEmpty()) {
            dOPasRandomUtilities.LOGGER.warn("{} recipe list was empty; using fallback", type.id());
            applyFallback(type);
            return;
        }

        List<GeneratorRecipe> parsed = new ArrayList<>();
        for (JsonElement element : recipeArray) {
            if (!element.isJsonObject()) {
                dOPasRandomUtilities.LOGGER.warn("Skipping non-object recipe entry in {}", type.id());
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = object.has("id") ? object.get("id").getAsString() : "<unknown>";
            parseDefinition(object, fileDefaultOutput, allowRandomResult).ifPresentOrElse(
                    parsed::add,
                    () -> dOPasRandomUtilities.LOGGER.warn(
                            "Skipping invalid recipe '{}' in {}",
                            id,
                            type.id()
                    )
            );
        }

        if (parsed.isEmpty()) {
            dOPasRandomUtilities.LOGGER.warn("No valid recipes in {}; using fallback", type.id());
            applyFallback(type);
            return;
        }

        parsed.sort(Comparator.comparingInt(GeneratorRecipe::specificity).reversed());
        RECIPE_MAP.put(type, List.copyOf(parsed));
        dOPasRandomUtilities.LOGGER.info("Loaded {} recipes for {}", parsed.size(), type.id());
    }

    private static Optional<GeneratorRecipe> parseDefinition(
            JsonObject definition,
            GeneratorOutputMode fileDefaultOutput,
            boolean allowRandomResult
    ) {
        if (!definition.has("id") || !definition.has("result")) {
            return Optional.empty();
        }

        String recipeId = definition.get("id").getAsString();
        if (recipeId.isBlank()) {
            return Optional.empty();
        }

        String resultRaw = definition.get("result").getAsString().trim();
        Block resultBlock = null;
        Fluid resultFluid = null;
        if (isRandomResultToken(resultRaw)) {
            if (!allowRandomResult) {
                dOPasRandomUtilities.LOGGER.warn(
                        "Recipe '{}' uses random result but this generator requires a fixed block id",
                        recipeId
                );
                return Optional.empty();
            }
        } else {
            Identifier resultId = Identifier.parse(resultRaw);
            // Prefer fluids when an id exists in both registries (e.g. water / lava).
            if (BuiltInRegistries.FLUID.containsKey(resultId)) {
                resultFluid = BuiltInRegistries.FLUID.getValue(resultId);
            } else if (BuiltInRegistries.BLOCK.containsKey(resultId)) {
                resultBlock = BuiltInRegistries.BLOCK.getValue(resultId);
            } else {
                dOPasRandomUtilities.LOGGER.warn("Unknown result '{}' in recipe '{}'", resultId, recipeId);
                return Optional.empty();
            }
        }

        List<GeneratorResource> resources = new ArrayList<>(GeneratorRecipe.SIDE_COUNT);
        boolean[] consume = new boolean[GeneratorRecipe.SIDE_COUNT];
        Arrays.fill(consume, false);

        JsonArray sides = definition.has("sides") && definition.get("sides").isJsonArray()
                ? definition.getAsJsonArray("sides")
                : new JsonArray();

        if (sides.size() > GeneratorRecipe.SIDE_COUNT) {
            dOPasRandomUtilities.LOGGER.warn(
                    "Recipe '{}' has {} side requirements but only {} horizontal sides are supported",
                    recipeId,
                    sides.size(),
                    GeneratorRecipe.SIDE_COUNT
            );
            return Optional.empty();
        }

        for (int i = 0; i < sides.size(); i++) {
            Optional<SideRequirement> side = parseSide(sides.get(i), recipeId, i);
            if (side.isEmpty()) {
                return Optional.empty();
            }
            resources.add(side.get().resource());
            consume[i] = side.get().consume();
        }
        while (resources.size() < GeneratorRecipe.SIDE_COUNT) {
            resources.add(null);
        }

        Block requiredUnder = null;
        if (definition.has("below") && !definition.get("below").isJsonNull()) {
            String belowId = definition.get("below").getAsString();
            if (!belowId.isBlank()) {
                Identifier underId = Identifier.parse(belowId);
                if (!BuiltInRegistries.BLOCK.containsKey(underId)) {
                    dOPasRandomUtilities.LOGGER.warn("Unknown below block '{}' in recipe '{}'", belowId, recipeId);
                    return Optional.empty();
                }
                requiredUnder = BuiltInRegistries.BLOCK.getValue(underId);
            }
        }

        int ticks = definition.has("ticks") ? definition.get("ticks").getAsInt() : 20;
        int amount = definition.has("amount") ? definition.get("amount").getAsInt() : 1;
        if (ticks <= 0) {
            ticks = 20;
        }
        if (amount <= 0) {
            amount = 1;
        }

        GeneratorOutputMode outputMode = definition.has("output")
                ? GeneratorOutputMode.parse(definition.get("output").getAsString())
                : fileDefaultOutput;

        return Optional.of(new GeneratorRecipe(
                recipeId,
                resultBlock,
                resultFluid,
                resources,
                consume,
                requiredUnder,
                ticks,
                amount,
                outputMode
        ));
    }

    private static boolean isRandomResultToken(String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("random")
                || normalized.equals("random_ore")
                || normalized.equals("random_metal")
                || normalized.equals("random_storage")
                || normalized.equals("*");
    }

    private static Optional<SideRequirement> parseSide(JsonElement element, String recipeId, int index) {
        String fieldName = "sides[" + index + "]";
        final String resourceId;
        final boolean consume;

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            resourceId = element.getAsString();
            consume = false;
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (!object.has("id")) {
                dOPasRandomUtilities.LOGGER.warn("Missing id in {} of recipe '{}'", fieldName, recipeId);
                return Optional.empty();
            }
            resourceId = object.get("id").getAsString();
            consume = object.has("consume") && object.get("consume").getAsBoolean();
        } else {
            dOPasRandomUtilities.LOGGER.warn("Invalid {} in recipe '{}'", fieldName, recipeId);
            return Optional.empty();
        }

        Optional<GeneratorResource> resource = resolveResource(resourceId, recipeId, fieldName);
        return resource.map(value -> new SideRequirement(value, consume));
    }

    private static Optional<GeneratorResource> resolveResource(
            @Nullable String resourceId,
            String recipeId,
            String fieldName
    ) {
        if (resourceId == null || resourceId.isBlank()) {
            dOPasRandomUtilities.LOGGER.warn("Empty {} in recipe '{}'", fieldName, recipeId);
            return Optional.empty();
        }

        Identifier id = Identifier.parse(resourceId);
        // Prefer fluids when an id exists in both registries (e.g. water / lava).
        if (BuiltInRegistries.FLUID.containsKey(id)) {
            Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
            return Optional.of(GeneratorResource.ofFluid(fluid));
        }
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            return Optional.of(GeneratorResource.ofBlock(block));
        }

        dOPasRandomUtilities.LOGGER.warn("Unknown {} '{}' in recipe '{}'", fieldName, resourceId, recipeId);
        return Optional.empty();
    }

    private static GeneratorRecipe createFallbackCobblestoneRecipe() {
        return new GeneratorRecipe(
                "cobblestone",
                BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:cobblestone")),
                null,
                Arrays.asList(null, null, null, null),
                new boolean[] {false, false, false, false},
                null,
                20,
                1,
                GeneratorOutputMode.INSERT
        );
    }

    private static GeneratorRecipe createFallbackRandomRecipe(GeneratorType type) {
        return new GeneratorRecipe(
                type.mode() == GeneratorType.Mode.METAL_BLOCK ? "random_storage" : "random_ore",
                null,
                null,
                Arrays.asList(null, null, null, null),
                new boolean[] {false, false, false, false},
                null,
                40,
                1,
                GeneratorOutputMode.PLACE
        );
    }

    private record SideRequirement(GeneratorResource resource, boolean consume) {}
}
