package com.dopa.randomutilities.config;

import com.dopa.randomutilities.dOPasRandomUtilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Discovers ore and compressed resource/metal storage blocks.
 * Storage detection requires a known material keyword and rejects organics/decorative variants.
 */
public final class GeneratedBlockLists {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.parse("c:ores"));
    private static final TagKey<Block> STORAGE_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.parse("c:storage_blocks"));

    /** Rejected even if tagged as storage blocks. */
    private static final Set<String> STORAGE_EXCLUSIONS = Set.of(
            "slime", "honey", "kelp", "hay", "bone", "dried", "magma", "clay",
            "snow", "bamboo", "cactus", "melon", "pumpkin", "wheat", "moss",
            "resin", "echo", "sculk", "chorus", "nether_wart", "warped_wart",
            "chiseled", "pillar", "smooth_", "cut_", "brick", "bricks", "tile",
            "tiles", "stairs", "slab", "wall", "fence", "door", "trapdoor",
            "generator", "machine", "command", "structure", "barrel", "chest",
            "exposed_", "weathered_", "oxidized_"
    );

    /**
     * Compressed "block of X" materials: metals + common ore products.
     * A storage candidate must match one of these (tag alone is not enough).
     */
    private static final Set<String> STORAGE_MATERIALS = Set.of(
            "iron", "gold", "copper", "netherite",
            "tin", "lead", "silver", "nickel", "zinc", "aluminum", "aluminium",
            "bronze", "steel", "osmium", "uranium", "platinum", "invar", "electrum",
            "brass", "constantan", "signalum", "lumium", "enderium",
            "coal", "lapis", "redstone", "diamond", "emerald", "quartz", "amethyst",
            "ruby", "sapphire", "topaz", "peridot", "certus", "fluix",
            "raw_iron", "raw_gold", "raw_copper"
    );

    private static final Set<Block> VANILLA_STORAGE_BLOCKS = createVanillaStorageBlocks();
    private static final Set<Block> VANILLA_ORES = createVanillaOres();

    private static List<Block> ores = List.of();
    private static List<Block> metalBlocks = List.of();

    private GeneratedBlockLists() {}

    private static Set<Block> createVanillaOres() {
        Set<Block> blocks = new HashSet<>();
        addAll(blocks,
                Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.COPPER_ORE, Blocks.COAL_ORE,
                Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE,
                Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS,
                Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE
        );
        return Set.copyOf(blocks);
    }

    private static Set<Block> createVanillaStorageBlocks() {
        Set<Block> blocks = new HashSet<>();
        addAll(blocks,
                Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.COPPER_BLOCK, Blocks.NETHERITE_BLOCK,
                Blocks.COAL_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK,
                Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.QUARTZ_BLOCK,
                Blocks.AMETHYST_BLOCK, Blocks.RAW_IRON_BLOCK, Blocks.RAW_GOLD_BLOCK,
                Blocks.RAW_COPPER_BLOCK
        );
        return Set.copyOf(blocks);
    }

    private static void addIfPresent(Set<Block> blocks, String id) {
        Identifier identifier = Identifier.parse(id);
        if (BuiltInRegistries.BLOCK.containsKey(identifier)) {
            blocks.add(BuiltInRegistries.BLOCK.getValue(identifier));
        }
    }

    private static void addAll(Set<Block> target, Block... blocks) {
        for (Block block : blocks) {
            target.add(block);
        }
    }

    public static void rebuild() {
        List<Block> foundOres = new ArrayList<>();
        List<Block> foundStorage = new ArrayList<>();
        Set<Block> oreSet = new HashSet<>();
        Set<Block> storageSet = new HashSet<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !hasItemForm(block)) {
                continue;
            }
            if (dOPasRandomUtilities.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("_generator")) {
                continue;
            }

            String path = id.getPath().toLowerCase(Locale.ROOT);
            var holder = block.builtInRegistryHolder();

            if (isOreBlock(holder.is(ORES), path, block) && oreSet.add(block)) {
                foundOres.add(block);
            }

            if (isStorageBlock(holder.is(STORAGE_BLOCKS), path, block) && storageSet.add(block)) {
                foundStorage.add(block);
            }
        }

        if (foundOres.isEmpty()) {
            foundOres.addAll(VANILLA_ORES);
        }
        if (foundStorage.isEmpty()) {
            foundStorage.addAll(VANILLA_STORAGE_BLOCKS);
        }

        ores = List.copyOf(foundOres);
        metalBlocks = List.copyOf(foundStorage);
        dOPasRandomUtilities.LOGGER.info(
                "Discovered {} ore blocks and {} metal/storage blocks for random generators",
                ores.size(),
                metalBlocks.size()
        );
    }

    public static List<Block> ores() {
        return ores;
    }

    public static List<Block> metalBlocks() {
        return metalBlocks;
    }

    private static boolean hasItemForm(Block block) {
        return block.asItem() != Items.AIR;
    }

    private static boolean isOreBlock(boolean inOresTag, String path, Block block) {
        if (VANILLA_ORES.contains(block)) {
            return true;
        }
        if (path.contains("generator") || path.contains("machine")) {
            return false;
        }
        boolean looksLikeOre = path.endsWith("_ore")
                || path.equals("ancient_debris")
                || path.endsWith("_debris");
        return inOresTag || looksLikeOre;
    }

    private static boolean isStorageBlock(boolean inStorageTag, String path, Block block) {
        if (VANILLA_STORAGE_BLOCKS.contains(block)) {
            return true;
        }
        if (containsAny(path, STORAGE_EXCLUSIONS)) {
            return false;
        }
        if (!matchesStorageMaterial(path)) {
            return false;
        }

        // Accept tagged storage blocks of known materials, or clear "block of X" names.
        if (inStorageTag) {
            return true;
        }
        return path.endsWith("_block")
                || path.startsWith("block_of_")
                || path.contains("storage_block");
    }

    private static boolean matchesStorageMaterial(String path) {
        for (String material : STORAGE_MATERIALS) {
            if (path.contains(material)) {
                // Avoid matching "redstone_lamp" / "redstone_torch" style non-storage names.
                if ("redstone".equals(material)
                        && !path.contains("redstone_block")
                        && !path.equals("block_of_redstone")
                        && !path.contains("storage_block")) {
                    // Allow path like "block_of_redstone" already handled; require block-ish form.
                    if (!(path.endsWith("_block") || path.startsWith("block_of_"))) {
                        continue;
                    }
                }
                return true;
            }
        }
        // Generic raw metal storage: raw_*_block
        return path.startsWith("raw_") && (path.endsWith("_block") || path.contains("storage_block"));
    }

    private static boolean containsAny(String path, Set<String> keywords) {
        for (String keyword : keywords) {
            if (path.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
