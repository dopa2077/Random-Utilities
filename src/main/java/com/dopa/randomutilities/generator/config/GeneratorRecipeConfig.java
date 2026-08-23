package com.dopa.randomutilities.generator.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.fluids.FluidType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GeneratorRecipeConfig {
    public static final TagKey<Block> RANDOM_ORES = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "random_ores")
    );
    public static final TagKey<Block> METAL_BLOCKS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "metal_blocks")
    );

    private static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, Identifier.parse("c:ores"));
    private static final TagKey<Block> C_STORAGE_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.parse("c:storage_blocks"));
    private static final Set<String> STORAGE_EXCLUSIONS = Set.of(
            "slime", "honey", "kelp", "hay", "bone", "dried", "magma", "clay",
            "snow", "bamboo", "cactus", "melon", "pumpkin", "wheat", "moss",
            "resin", "echo", "sculk", "chorus", "nether_wart", "warped_wart",
            "chiseled", "pillar", "smooth_", "cut_", "brick", "bricks", "tile",
            "tiles", "stairs", "slab", "wall", "fence", "door", "trapdoor",
            "generator", "machine", "command", "structure", "barrel", "chest",
            "exposed_", "weathered_", "oxidized_", "lamp", "torch", "ore"
    );
    private static final Set<Block> VANILLA_ORES = Set.of(
            Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.COPPER_ORE, Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS,
            Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE
    );
    private static final Set<Block> VANILLA_STORAGE_BLOCKS = createVanillaStorageBlocks();

    private static final Map<GeneratorType, List<GeneratorRecipe>> RECIPE_MAP = new EnumMap<>(GeneratorType.class);
    private static List<Block> ores = List.of();
    private static List<Block> metalBlocks = List.of();

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
                dOPasRandomUtilities.LOGGER.error("Failed to load generator config from {}", configFile, exception);
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

    public static List<Block> ores() {
        return ores;
    }

    public static List<Block> metalBlocks() {
        return metalBlocks;
    }

    public static void rebuildBlockPools() {
        Set<Block> oreSet = new HashSet<>();
        Set<Block> storageSet = new HashSet<>();
        collectTagged(RANDOM_ORES, oreSet);
        collectTagged(METAL_BLOCKS, storageSet);
        collectTagged(C_ORES, oreSet);

        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || block.asItem() == Items.AIR) {
                continue;
            }
            if (dOPasRandomUtilities.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("_generator")) {
                continue;
            }
            if (block.builtInRegistryHolder().is(C_STORAGE_BLOCKS) && isAcceptableStorage(id.getPath())) {
                storageSet.add(block);
            }
        }

        if (oreSet.isEmpty()) {
            oreSet.addAll(VANILLA_ORES);
        }
        if (storageSet.isEmpty()) {
            storageSet.addAll(VANILLA_STORAGE_BLOCKS);
        }

        ores = List.copyOf(oreSet);
        metalBlocks = List.copyOf(storageSet);
        dOPasRandomUtilities.LOGGER.info(
                "Random generator pools: {} ores, {} metal/storage blocks",
                ores.size(),
                metalBlocks.size()
        );
    }

    private static Set<Block> createVanillaStorageBlocks() {
        Set<Block> blocks = new HashSet<>(Set.of(
                Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.COAL_BLOCK,
                Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK,
                Blocks.QUARTZ_BLOCK, Blocks.AMETHYST_BLOCK, Blocks.RAW_IRON_BLOCK, Blocks.RAW_GOLD_BLOCK,
                Blocks.RAW_COPPER_BLOCK
        ));
        Identifier copper = Identifier.parse("minecraft:copper_block");
        if (BuiltInRegistries.BLOCK.containsKey(copper)) {
            blocks.add(BuiltInRegistries.BLOCK.getValue(copper));
        }
        return Set.copyOf(blocks);
    }

    private static void collectTagged(TagKey<Block> tag, Set<Block> target) {
        BuiltInRegistries.BLOCK.getTagOrEmpty(tag).forEach(holder -> {
            Block block = holder.value();
            if (block.asItem() != Items.AIR) {
                target.add(block);
            }
        });
    }

    private static boolean isAcceptableStorage(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        for (String excluded : STORAGE_EXCLUSIONS) {
            if (lower.contains(excluded)) {
                return false;
            }
        }
        return true;
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
            dOPasRandomUtilities.LOGGER.error("Failed to load bundled default config for {}", type.id(), exception);
            applyFallback(type);
        }
    }

    private static void applyFallback(GeneratorType type) {
        RECIPE_MAP.put(type, List.of(switch (type.mode()) {
            case RECIPE -> createFallbackCobblestoneRecipe();
            case RANDOM_ORE, METAL_BLOCK -> createFallbackRandomRecipe(type);
        }));
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
        int ticks = Math.max(1, object.has("ticks") ? object.get("ticks").getAsInt() : 40);
        int amount = parseOutputAmount(object, false);
        GeneratorOutputMode outputMode = object.has("output")
                ? GeneratorOutputMode.parse(object.get("output").getAsString())
                : GeneratorOutputMode.PLACE;
        RECIPE_MAP.put(type, List.of(new GeneratorRecipe(
                "default", null, null, null,
                Arrays.asList(null, null, null, null),
                new boolean[] {false, false, false, false},
                null, ticks, amount, outputMode
        )));
    }

    private static void parseRecipeFile(GeneratorType type, JsonElement root, boolean allowRandomResult) {
        JsonArray recipeArray;
        GeneratorOutputMode fileDefaultOutput = allowRandomResult ? GeneratorOutputMode.PLACE : GeneratorOutputMode.INSERT;
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
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = object.has("id") ? object.get("id").getAsString() : "<unknown>";
            parseDefinition(object, fileDefaultOutput, allowRandomResult).ifPresentOrElse(
                    parsed::add,
                    () -> dOPasRandomUtilities.LOGGER.warn("Skipping invalid recipe '{}' in {}", id, type.id())
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
        Item resultItem = null;
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
            if (BuiltInRegistries.FLUID.containsKey(resultId)) {
                resultFluid = BuiltInRegistries.FLUID.getValue(resultId);
            } else if (BuiltInRegistries.BLOCK.containsKey(resultId)) {
                resultBlock = BuiltInRegistries.BLOCK.getValue(resultId);
            } else if (BuiltInRegistries.ITEM.containsKey(resultId)) {
                resultItem = BuiltInRegistries.ITEM.getValue(resultId);
                if (resultItem == Items.AIR) {
                    dOPasRandomUtilities.LOGGER.warn("Unknown result '{}' in recipe '{}'", resultId, recipeId);
                    return Optional.empty();
                }
            } else {
                dOPasRandomUtilities.LOGGER.warn("Unknown result '{}' in recipe '{}'", resultId, recipeId);
                return Optional.empty();
            }
        }

        List<GeneratorResource> resources = new ArrayList<>(GeneratorRecipe.SIDE_COUNT);
        boolean[] consume = new boolean[GeneratorRecipe.SIDE_COUNT];
        JsonArray sides = definition.has("sides") && definition.get("sides").isJsonArray()
                ? definition.getAsJsonArray("sides")
                : new JsonArray();
        if (sides.size() > GeneratorRecipe.SIDE_COUNT) {
            dOPasRandomUtilities.LOGGER.warn(
                    "Recipe '{}' has {} side requirements but only {} horizontal sides are supported",
                    recipeId, sides.size(), GeneratorRecipe.SIDE_COUNT
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

        int ticks = Math.max(1, definition.has("ticks") ? definition.get("ticks").getAsInt() : 20);
        int amount = parseOutputAmount(definition, resultFluid != null);
        GeneratorOutputMode outputMode = definition.has("output")
                ? GeneratorOutputMode.parse(definition.get("output").getAsString())
                : fileDefaultOutput;
        // Items cannot be placed as blocks; INSERT into the inventory above instead.
        if (resultItem != null && outputMode == GeneratorOutputMode.PLACE) {
            outputMode = GeneratorOutputMode.INSERT;
        }

        return Optional.of(new GeneratorRecipe(
                recipeId, resultBlock, resultItem, resultFluid, resources, consume,
                requiredUnder, ticks, amount, outputMode
        ));
    }

    /**
     * Reads {@code amount} from JSON. Block/item recipes use whole items; fluid recipes use buckets
     * (including fractions such as {@code 0.2}) and are stored as millibuckets.
     */
    private static int parseOutputAmount(JsonObject definition, boolean fluidResult) {
        double raw = 1.0D;
        if (definition.has("amount") && !definition.get("amount").isJsonNull()) {
            try {
                raw = definition.get("amount").getAsDouble();
            } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException ignored) {
                raw = 1.0D;
            }
        }
        if (fluidResult) {
            long millibuckets = Math.round(raw * FluidType.BUCKET_VOLUME);
            if (millibuckets > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) Math.max(1L, millibuckets);
        }
        long items = Math.round(raw);
        if (items > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(1L, items);
    }

    private static boolean isRandomResultToken(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("random") || normalized.equals("random_ore")
                || normalized.equals("random_metal") || normalized.equals("random_storage")
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
        return resolveResource(resourceId, recipeId, fieldName).map(value -> new SideRequirement(value, consume));
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
        if (BuiltInRegistries.FLUID.containsKey(id)) {
            return Optional.of(GeneratorResource.ofFluid(BuiltInRegistries.FLUID.getValue(id)));
        }
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            return Optional.of(GeneratorResource.ofBlock(BuiltInRegistries.BLOCK.getValue(id)));
        }
        dOPasRandomUtilities.LOGGER.warn("Unknown {} '{}' in recipe '{}'", fieldName, resourceId, recipeId);
        return Optional.empty();
    }

    private static GeneratorRecipe createFallbackCobblestoneRecipe() {
        return new GeneratorRecipe(
                "cobblestone",
                BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:cobblestone")),
                null,
                null,
                Arrays.asList(null, null, null, null),
                new boolean[] {false, false, false, false},
                null, 20, 1, GeneratorOutputMode.INSERT
        );
    }

    private static GeneratorRecipe createFallbackRandomRecipe(GeneratorType type) {
        return new GeneratorRecipe(
                type.mode() == GeneratorType.Mode.METAL_BLOCK ? "random_storage" : "random_ore",
                null, null, null,
                Arrays.asList(null, null, null, null),
                new boolean[] {false, false, false, false},
                null, 40, 1, GeneratorOutputMode.PLACE
        );
    }

    private record SideRequirement(GeneratorResource resource, boolean consume) {}
}
