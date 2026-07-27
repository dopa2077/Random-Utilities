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
import java.util.List;
import java.util.Optional;

public final class GeneratorRecipeConfig {
    private static final String CONFIG_PATH =
            "dopas_random_utilities/blocks/resource_generator/basic_stone_generator.json";
    private static final String DEFAULT_RESOURCE =
            "/default/dopas_random_utilities/blocks/resource_generator/basic_stone_generator.json";

    private static List<GeneratorRecipe> recipes = List.of();
    private static GeneratorRecipe defaultRecipe = null;

    private GeneratorRecipeConfig() {}

    public static void load() {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_PATH);
        Path configDir = configFile.getParent();

        try {
            Files.createDirectories(configDir);
            if (Files.notExists(configFile)) {
                copyDefaultConfig(configFile);
            }

            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                parseAndApply(reader);
            }
        } catch (IOException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load generator recipes from {}", configFile, exception);
            loadDefaultFromJar();
        }
    }

    public static void reload() {
        load();
    }

    public static List<GeneratorRecipe> getRecipes() {
        return recipes;
    }

    public static GeneratorRecipe getDefaultRecipe() {
        return defaultRecipe;
    }

    public static Optional<GeneratorRecipe> getRecipeById(String id) {
        return recipes.stream().filter(recipe -> recipe.id().equals(id)).findFirst();
    }

    private static void copyDefaultConfig(Path configFile) throws IOException {
        try (InputStream input = GeneratorRecipeConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled default recipes at " + DEFAULT_RESOURCE);
            }
            Files.copy(input, configFile, StandardCopyOption.REPLACE_EXISTING);
            dOPasRandomUtilities.LOGGER.info("Wrote generator recipes config at {}", configFile);
        }
    }

    private static void loadDefaultFromJar() {
        try (InputStream input = GeneratorRecipeConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                recipes = List.of(createFallbackCobblestoneRecipe());
                defaultRecipe = recipes.getFirst();
                return;
            }
            parseAndApply(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled default generator recipes", exception);
            recipes = List.of(createFallbackCobblestoneRecipe());
            defaultRecipe = recipes.getFirst();
        }
    }

    private static void parseAndApply(Reader reader) {
        JsonArray recipeArray;
        try {
            JsonElement root = JsonParser.parseReader(reader);
            if (root.isJsonArray()) {
                recipeArray = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonElement recipesElement = root.getAsJsonObject().get("recipes");
                if (recipesElement == null || !recipesElement.isJsonArray()) {
                    throw new JsonSyntaxException("Expected a 'recipes' array");
                }
                recipeArray = recipesElement.getAsJsonArray();
            } else {
                throw new JsonSyntaxException("Expected recipe object or array");
            }
        } catch (JsonSyntaxException | IllegalStateException exception) {
            dOPasRandomUtilities.LOGGER.error("Invalid generator recipe JSON syntax", exception);
            recipes = List.of(createFallbackCobblestoneRecipe());
            defaultRecipe = recipes.getFirst();
            return;
        }

        if (recipeArray.isEmpty()) {
            dOPasRandomUtilities.LOGGER.warn("Generator recipe list was empty; using built-in cobblestone fallback");
            recipes = List.of(createFallbackCobblestoneRecipe());
            defaultRecipe = recipes.getFirst();
            return;
        }

        List<GeneratorRecipe> parsed = new ArrayList<>();
        for (JsonElement element : recipeArray) {
            if (!element.isJsonObject()) {
                dOPasRandomUtilities.LOGGER.warn("Skipping non-object generator recipe entry");
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = object.has("id") ? object.get("id").getAsString() : "<unknown>";
            parseDefinition(object).ifPresentOrElse(
                    parsed::add,
                    () -> dOPasRandomUtilities.LOGGER.warn("Skipping invalid generator recipe entry: {}", id)
            );
        }

        if (parsed.isEmpty()) {
            dOPasRandomUtilities.LOGGER.warn("No valid generator recipes found; using built-in cobblestone fallback");
            recipes = List.of(createFallbackCobblestoneRecipe());
            defaultRecipe = recipes.getFirst();
            return;
        }

        parsed.sort(Comparator.comparingInt(GeneratorRecipe::specificity).reversed());
        recipes = List.copyOf(parsed);
        defaultRecipe = recipes.stream()
                .filter(recipe -> "cobblestone".equals(recipe.id()))
                .findFirst()
                .orElse(recipes.getLast());
        dOPasRandomUtilities.LOGGER.info("Loaded {} basic stone generator recipes", recipes.size());
    }

    private static Optional<GeneratorRecipe> parseDefinition(JsonObject definition) {
        if (!definition.has("id") || !definition.has("result")) {
            return Optional.empty();
        }

        String recipeId = definition.get("id").getAsString();
        if (recipeId.isBlank()) {
            return Optional.empty();
        }

        Identifier resultId = Identifier.parse(definition.get("result").getAsString());
        if (!BuiltInRegistries.BLOCK.containsKey(resultId)) {
            dOPasRandomUtilities.LOGGER.warn("Unknown result block '{}' in recipe '{}'", resultId, recipeId);
            return Optional.empty();
        }
        Block resultBlock = BuiltInRegistries.BLOCK.getValue(resultId);

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

        return Optional.of(new GeneratorRecipe(
                recipeId,
                resultBlock,
                resources,
                consume,
                requiredUnder,
                ticks,
                amount
        ));
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
                Arrays.asList(null, null, null, null),
                new boolean[] {false, false, false, false},
                null,
                20,
                1
        );
    }

    private record SideRequirement(GeneratorResource resource, boolean consume) {}
}
