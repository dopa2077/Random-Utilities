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
 * Builds ore / metal pools for random generators.
 * Prefer datapack tags; fall back to common convention tags and vanilla defaults.
 */
public final class GeneratedBlockLists {
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

    private static List<Block> ores = List.of();
    private static List<Block> metalBlocks = List.of();

    private GeneratedBlockLists() {}

    private static Set<Block> createVanillaStorageBlocks() {
        Set<Block> blocks = new HashSet<>();
        blocks.add(Blocks.IRON_BLOCK);
        blocks.add(Blocks.GOLD_BLOCK);
        blocks.add(Blocks.NETHERITE_BLOCK);
        blocks.add(Blocks.COAL_BLOCK);
        blocks.add(Blocks.LAPIS_BLOCK);
        blocks.add(Blocks.REDSTONE_BLOCK);
        blocks.add(Blocks.DIAMOND_BLOCK);
        blocks.add(Blocks.EMERALD_BLOCK);
        blocks.add(Blocks.QUARTZ_BLOCK);
        blocks.add(Blocks.AMETHYST_BLOCK);
        blocks.add(Blocks.RAW_IRON_BLOCK);
        blocks.add(Blocks.RAW_GOLD_BLOCK);
        blocks.add(Blocks.RAW_COPPER_BLOCK);
        Identifier copper = Identifier.parse("minecraft:copper_block");
        if (BuiltInRegistries.BLOCK.containsKey(copper)) {
            blocks.add(BuiltInRegistries.BLOCK.getValue(copper));
        }
        return Set.copyOf(blocks);
    }

    public static void rebuild() {
        Set<Block> oreSet = new HashSet<>();
        Set<Block> storageSet = new HashSet<>();

        collectTagged(RANDOM_ORES, oreSet);
        collectTagged(METAL_BLOCKS, storageSet);
        collectTagged(C_ORES, oreSet);

        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !hasItemForm(block)) {
                continue;
            }
            if (dOPasRandomUtilities.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("_generator")) {
                continue;
            }

            var holder = block.builtInRegistryHolder();
            if (holder.is(C_STORAGE_BLOCKS) && isAcceptableStorage(id.getPath())) {
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

    private static void collectTagged(TagKey<Block> tag, Set<Block> target) {
        BuiltInRegistries.BLOCK.getTagOrEmpty(tag).forEach(holder -> {
            Block block = holder.value();
            if (hasItemForm(block)) {
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

    public static List<Block> ores() {
        return ores;
    }

    public static List<Block> metalBlocks() {
        return metalBlocks;
    }

    private static boolean hasItemForm(Block block) {
        return block.asItem() != Items.AIR;
    }
}
