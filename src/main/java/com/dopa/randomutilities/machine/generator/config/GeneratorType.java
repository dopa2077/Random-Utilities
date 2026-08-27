package com.dopa.randomutilities.machine.generator.config;

/**
 * All resource generators and how their configs are interpreted.
 */
public enum GeneratorType {
    BASIC_STONE("basic_stone_generator", Mode.RECIPE),
    INTERMEDIATE_STONE("intermediate_stone_generator", Mode.RECIPE),
    ADVANCED_STONE("advanced_stone_generator", Mode.RECIPE),
    ELITE_STONE("elite_stone_generator", Mode.RECIPE),
    ULTIMATE_STONE("ultimate_stone_generator", Mode.RECIPE),
    CREATIVE_STONE("creative_stone_generator", Mode.RECIPE),
    RANDOM_ORE("random_ore_generator", Mode.RANDOM_ORE),
    METAL_BLOCK("metal_block_generator", Mode.METAL_BLOCK),
    CREATIVE_RANDOM_ORE("creative_random_ore_generator", Mode.RANDOM_ORE),
    CREATIVE_METAL_BLOCK("creative_metal_block_generator", Mode.METAL_BLOCK);

    public enum Mode {
        RECIPE,
        RANDOM_ORE,
        METAL_BLOCK
    }

    private final String id;
    private final Mode mode;

    GeneratorType(String id, Mode mode) {
        this.id = id;
        this.mode = mode;
    }

    public String id() {
        return id;
    }

    public Mode mode() {
        return mode;
    }

    public String configFileName() {
        return id + ".json";
    }

    public String configRelativePath() {
        return "dopas_random_utilities/blocks/resource_generator/" + configFileName();
    }

    public String defaultResourcePath() {
        return "/default/dopas_random_utilities/blocks/resource_generator/" + configFileName();
    }
}
