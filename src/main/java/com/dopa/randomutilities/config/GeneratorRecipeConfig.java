package com.dopa.randomutilities.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class GeneratorRecipeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type RECIPE_LIST_TYPE = new TypeToken<List<RecipeDefinition>>() {}.getType();
    private static final String CONFIG_DIR = "basicstonegenerator";
    private static final String CONFIG_FILE = "recipes.json";
    private static final String DEFAULT_RESOURCE = "/default/basicstonegenerator/recipes.json";

    private static List<GeneratorRecipe> recipes = List.of();
    private static GeneratorRecipe defaultRecipe = null;

    private GeneratorRecipeConfig() {}

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR);
        Path configFile = configDir.resolve(CONFIG_FILE);

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
            Files.copy(input, configFile);
            dOPasRandomUtilities.LOGGER.info("Created default generator recipes at {}", configFile);
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
        List<RecipeDefinition> definitions;
        try {
            definitions = GSON.fromJson(reader, RECIPE_LIST_TYPE);
        } catch (JsonSyntaxException exception) {
            dOPasRandomUtilities.LOGGER.error("Invalid generator recipe JSON syntax", exception);
            loadDefaultFromJar();
            return;
        }

        if (definitions == null || definitions.isEmpty()) {
            dOPasRandomUtilities.LOGGER.warn("Generator recipe list was empty; using built-in cobblestone fallback");
            recipes = List.of(createFallbackCobblestoneRecipe());
            defaultRecipe = recipes.getFirst();
            return;
        }

        List<GeneratorRecipe> parsed = new ArrayList<>();
        for (RecipeDefinition definition : definitions) {
            parseDefinition(definition).ifPresentOrElse(
                    parsed::add,
                    () -> dOPasRandomUtilities.LOGGER.warn("Skipping invalid generator recipe entry: {}", definition.id)
            );
        }

        if (parsed.isEmpty()) {
            dOPasRandomUtilities.LOGGER.warn("No valid generator recipes found; using built-in cobblestone fallback");
            recipes = List.of(createFallbackCobblestoneRecipe());
            defaultRecipe = recipes.getFirst();
            return;
        }

        parsed.sort(Comparator.comparingInt(GeneratorRecipe::priority).reversed());
        recipes = List.copyOf(parsed);
        defaultRecipe = recipes.stream()
                .filter(recipe -> "cobblestone".equals(recipe.id()))
                .findFirst()
                .orElse(recipes.getLast());
        dOPasRandomUtilities.LOGGER.info("Loaded {} basic stone generator recipes", recipes.size());
    }

    private static Optional<GeneratorRecipe> parseDefinition(RecipeDefinition definition) {
        if (definition.id == null || definition.id.isBlank()) {
            return Optional.empty();
        }
        if (definition.result == null || definition.fluid1 == null || definition.fluid2 == null) {
            return Optional.empty();
        }

        Identifier resultId = Identifier.parse(definition.result);
        Identifier fluid1Id = Identifier.parse(definition.fluid1);
        Identifier fluid2Id = Identifier.parse(definition.fluid2);

        if (!BuiltInRegistries.BLOCK.containsKey(resultId)
                || !BuiltInRegistries.FLUID.containsKey(fluid1Id)
                || !BuiltInRegistries.FLUID.containsKey(fluid2Id)) {
            dOPasRandomUtilities.LOGGER.warn(
                    "Unknown registry entry in recipe '{}': result={}, fluid1={}, fluid2={}",
                    definition.id,
                    definition.result,
                    definition.fluid1,
                    definition.fluid2
            );
            return Optional.empty();
        }

        Block resultBlock = BuiltInRegistries.BLOCK.getValue(resultId);
        Fluid fluid1 = BuiltInRegistries.FLUID.getValue(fluid1Id);
        Fluid fluid2 = BuiltInRegistries.FLUID.getValue(fluid2Id);

        Block requiredUnder = null;
        if (definition.required_under != null && !definition.required_under.isBlank()) {
            Identifier underId = Identifier.parse(definition.required_under);
            if (!BuiltInRegistries.BLOCK.containsKey(underId)) {
                dOPasRandomUtilities.LOGGER.warn(
                        "Unknown required_under block '{}' in recipe '{}'",
                        definition.required_under,
                        definition.id
                );
                return Optional.empty();
            }
            requiredUnder = BuiltInRegistries.BLOCK.getValue(underId);
        }

        int ticks = definition.ticks > 0 ? definition.ticks : 20;
        int priority = definition.priority;

        return Optional.of(new GeneratorRecipe(
                definition.id,
                resultBlock,
                fluid1,
                fluid2,
                definition.consume1,
                definition.consume2,
                requiredUnder,
                ticks,
                priority
        ));
    }

    private static GeneratorRecipe createFallbackCobblestoneRecipe() {
        return new GeneratorRecipe(
                "cobblestone",
                BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:cobblestone")),
                BuiltInRegistries.FLUID.getValue(Identifier.parse("minecraft:lava")),
                BuiltInRegistries.FLUID.getValue(Identifier.parse("minecraft:water")),
                false,
                false,
                null,
                20,
                0
        );
    }

    @SuppressWarnings("unused")
    private static final class RecipeDefinition {
        String id;
        String result;
        String fluid1;
        String fluid2;
        boolean consume1;
        boolean consume2;
        @Nullable String required_under;
        int ticks = 20;
        int priority = 0;
    }
}
