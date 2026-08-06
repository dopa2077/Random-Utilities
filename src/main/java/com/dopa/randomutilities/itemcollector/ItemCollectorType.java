package com.dopa.randomutilities.itemcollector;

import com.dopa.randomutilities.registry.ModBlocks;

import net.minecraft.world.level.block.Block;

public enum ItemCollectorType {
    BASIC(
            "basic_item_collector",
            3,
            5,
            20,
            64,
            false,
            false
    ),
    ADVANCED(
            "advanced_item_collector",
            8,
            10,
            1,
            256,
            true,
            true
    );

    private final String id;
    private final int filterSlotCount;
    private final int maxRange;
    private final int minPickupDelay;
    private final int maxPickupBatch;
    private final boolean supportsWhitelist;
    private final boolean supportsLineOfSight;

    ItemCollectorType(
            String id,
            int filterSlotCount,
            int maxRange,
            int minPickupDelay,
            int maxPickupBatch,
            boolean supportsWhitelist,
            boolean supportsLineOfSight
    ) {
        this.id = id;
        this.filterSlotCount = filterSlotCount;
        this.maxRange = maxRange;
        this.minPickupDelay = minPickupDelay;
        this.maxPickupBatch = maxPickupBatch;
        this.supportsWhitelist = supportsWhitelist;
        this.supportsLineOfSight = supportsLineOfSight;
    }

    public String id() {
        return id;
    }

    public int filterSlotCount() {
        return filterSlotCount;
    }

    public int maxRange() {
        return maxRange;
    }

    public int minPickupDelay() {
        return minPickupDelay;
    }

    public int maxPickupBatch() {
        return maxPickupBatch;
    }

    public boolean supportsWhitelist() {
        return supportsWhitelist;
    }

    public boolean supportsLineOfSight() {
        return supportsLineOfSight;
    }

    public Block block() {
        return switch (this) {
            case BASIC -> ModBlocks.BASIC_ITEM_COLLECTOR.get();
            case ADVANCED -> ModBlocks.ADVANCED_ITEM_COLLECTOR.get();
        };
    }

    public static ItemCollectorType fromBlock(Block block) {
        if (block == ModBlocks.ADVANCED_ITEM_COLLECTOR.get()) {
            return ADVANCED;
        }
        return BASIC;
    }

    public int clampRange(int value) {
        return Math.max(0, Math.min(maxRange, value));
    }

    public int clampPickupDelay(int value) {
        if (value == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(minPickupDelay, value);
    }

    public int clampPickupBatch(int value) {
        return Math.max(1, Math.min(maxPickupBatch, value));
    }
}
