package com.dopa.randomutilities.machine.generator;

import com.dopa.randomutilities.machine.generator.config.GeneratorOutputMode;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipe;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Item/fluid/place/drop output routing for {@link ResourceGeneratorBlockEntity}. */
final class ResourceGeneratorOutput {
    private static final int MAX_NEARBY_ITEMS = 24;

    private ResourceGeneratorOutput() {}

    static boolean canStart(
            ServerLevel level,
            BlockPos pos,
            GeneratorType type,
            GeneratorRecipe recipe,
            int amount,
            @Nullable Block outputOverride
    ) {
        // Fluids always insert as fluid — never treat the GUI display proxy (ice/magma) as item output.
        if (recipe.isFluidResult()) {
            return insertFluid(level, pos, recipe.resultFluid(), amount, true) >= amount;
        }
        GeneratorOutputMode mode = recipe.outputMode();
        int requested = requestedAmount(mode, amount);
        if (outputOverride != null) {
            return canOutput(level, pos, outputOverride, requested, mode);
        }
        if (recipe.isRandomResult()) {
            if (poolFor(type).isEmpty()) {
                return false;
            }
            return switch (mode) {
                case INSERT -> getItemOutputHandler(level, pos) != null;
                case DROP -> canAcceptItemDrop(level, pos);
                case PLACE -> canAcceptPlace(level, pos);
            };
        }
        if (recipe.isItemResult()) {
            return canOutputItem(level, pos, recipe.resultItem(), requested, mode);
        }
        return canOutput(level, pos, recipe.result(), requested, mode);
    }

    /** PLACE always outputs one block; productivity does not multiply placed blocks. */
    static int requestedAmount(GeneratorOutputMode mode, int amount) {
        return mode == GeneratorOutputMode.PLACE ? 1 : Math.max(0, amount);
    }

    private static boolean canOutput(
            ServerLevel level,
            BlockPos pos,
            @Nullable Block result,
            int amount,
            GeneratorOutputMode mode
    ) {
        if (result == null || amount <= 0) {
            return false;
        }
        return switch (mode) {
            case INSERT -> insertItems(level, pos, result.asItem(), amount, true) >= amount;
            case DROP -> canAcceptItemDrop(level, pos);
            case PLACE -> canAcceptPlace(level, pos);
        };
    }

    private static boolean canOutputItem(
            ServerLevel level,
            BlockPos pos,
            @Nullable Item item,
            int amount,
            GeneratorOutputMode mode
    ) {
        if (item == null || item == Items.AIR || amount <= 0) {
            return false;
        }
        GeneratorOutputMode effective = mode == GeneratorOutputMode.PLACE ? GeneratorOutputMode.INSERT : mode;
        return switch (effective) {
            case INSERT -> insertItems(level, pos, item, amount, true) >= amount;
            case DROP -> canAcceptItemDrop(level, pos);
            case PLACE -> false;
        };
    }

    static int outputBlocks(
            ServerLevel level,
            BlockPos pos,
            @Nullable Block result,
            int amount,
            GeneratorOutputMode mode,
            boolean simulate
    ) {
        if (result == null || amount <= 0) {
            return 0;
        }
        return switch (mode) {
            case INSERT -> insertItems(level, pos, result.asItem(), amount, simulate);
            case DROP -> simulate || dropItems(level, pos, result.asItem(), amount) ? amount : 0;
            case PLACE -> simulate ? (canAcceptPlace(level, pos) ? 1 : 0) : (placeBlock(level, pos, result) ? 1 : 0);
        };
    }

    /**
     * Item-only results cannot be placed. PLACE is treated as INSERT so a misconfigured
     * recipe still produces into the inventory above.
     */
    static int outputItems(
            ServerLevel level,
            BlockPos pos,
            @Nullable Item item,
            int amount,
            GeneratorOutputMode mode,
            boolean simulate
    ) {
        if (item == null || item == Items.AIR || amount <= 0) {
            return 0;
        }
        GeneratorOutputMode effective = mode == GeneratorOutputMode.PLACE ? GeneratorOutputMode.INSERT : mode;
        return switch (effective) {
            case INSERT -> insertItems(level, pos, item, amount, simulate);
            case DROP -> simulate || dropItems(level, pos, item, amount) ? amount : 0;
            case PLACE -> 0;
        };
    }

    static int insertItems(ServerLevel level, BlockPos pos, Item item, int amount, boolean simulate) {
        ResourceHandler<ItemResource> handler = getItemOutputHandler(level, pos);
        if (handler == null || item == null || item == Items.AIR || amount <= 0) {
            return 0;
        }
        ItemResource resource = ItemResource.of(item);
        if (resource.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.open(null)) {
            int inserted = handler.insert(resource, amount, tx);
            if (simulate) {
                return Math.max(0, inserted);
            }
            if (inserted >= amount) {
                tx.commit();
                return inserted;
            }
            return 0;
        }
    }

    /**
     * Inserts fluid into the tank above. {@code millibuckets} is the requested volume
     * (1000 = one bucket). Returns millibuckets actually inserted.
     */
    static int insertFluid(ServerLevel level, BlockPos pos, @Nullable Fluid fluid, int millibuckets, boolean simulate) {
        ResourceHandler<FluidResource> handler = getFluidOutputHandler(level, pos);
        if (handler == null || fluid == null || fluid.isSame(Fluids.EMPTY) || millibuckets <= 0) {
            return 0;
        }
        FluidResource resource = FluidResource.of(fluid);
        if (resource.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.open(null)) {
            int insertedMb = handler.insert(resource, millibuckets, tx);
            if (simulate) {
                return Math.max(0, insertedMb);
            }
            if (insertedMb >= millibuckets) {
                tx.commit();
                return insertedMb;
            }
            return 0;
        }
    }

    static Block displayBlockForFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return Blocks.GLASS;
        }
        if (fluid.isSame(Fluids.WATER)) {
            return Blocks.ICE;
        }
        if (fluid.isSame(Fluids.LAVA)) {
            return Blocks.MAGMA_BLOCK;
        }
        return Blocks.GLASS;
    }

    @Nullable
    static Block resolveResult(ServerLevel level, GeneratorType type, GeneratorRecipe recipe) {
        if (!recipe.isRandomResult()) {
            return recipe.result();
        }
        List<Block> pool = poolFor(type);
        return pool.isEmpty() ? null : pool.get(level.getRandom().nextInt(pool.size()));
    }

    static List<Block> poolFor(GeneratorType type) {
        return switch (type.mode()) {
            case RANDOM_ORE -> GeneratorRecipeConfig.ores();
            case METAL_BLOCK -> GeneratorRecipeConfig.metalBlocks();
            case RECIPE -> List.of();
        };
    }

    @Nullable
    private static ResourceHandler<ItemResource> getItemOutputHandler(ServerLevel level, BlockPos pos) {
        return level.getCapability(Capabilities.Item.BLOCK, pos.above(), Direction.DOWN);
    }

    @Nullable
    private static ResourceHandler<FluidResource> getFluidOutputHandler(ServerLevel level, BlockPos pos) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos.above(), Direction.DOWN);
    }

    private static boolean canAcceptPlace(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.above()).canBeReplaced();
    }

    private static boolean canAcceptItemDrop(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        if (!level.getBlockState(above).canBeReplaced()) {
            return false;
        }
        int count = 0;
        for (ItemEntity ignored : level.getEntitiesOfClass(ItemEntity.class, new AABB(above).inflate(1.5))) {
            if (++count >= MAX_NEARBY_ITEMS) {
                return false;
            }
        }
        return true;
    }

    private static boolean placeBlock(ServerLevel level, BlockPos pos, Block result) {
        BlockPos above = pos.above();
        return canAcceptPlace(level, pos)
                && level.setBlock(above, result.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean dropItems(ServerLevel level, BlockPos pos, Item item, int amount) {
        if (!canAcceptItemDrop(level, pos)) {
            return false;
        }
        ItemStack probe = new ItemStack(item, 1);
        if (probe.isEmpty()) {
            return false;
        }
        BlockPos above = pos.above();
        double x = above.getX() + 0.5;
        double y = above.getY() + 0.15;
        double z = above.getZ() + 0.5;
        int maxStack = Math.max(1, probe.getMaxStackSize());
        for (int remaining = amount; remaining > 0; ) {
            int batch = Math.min(remaining, maxStack);
            ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(item, batch));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            remaining -= batch;
        }
        return true;
    }

    /** Block shown inside the glass for item-only results (ores stand in for ingots/gems). */
    static Block previewBlockForItem(@Nullable Item item) {
        if (item == null || item == Items.AIR) {
            return Blocks.COBBLESTONE;
        }
        if (item instanceof BlockItem blockItem) {
            Block asBlock = blockItem.getBlock();
            if (asBlock != Blocks.AIR) {
                return asBlock;
            }
        }
        if (item == Items.COPPER_INGOT) {
            return Blocks.COPPER_ORE;
        }
        if (item == Items.IRON_INGOT) {
            return Blocks.IRON_ORE;
        }
        if (item == Items.GOLD_INGOT) {
            return Blocks.NETHER_GOLD_ORE;
        }
        if (item == Items.DIAMOND) {
            return Blocks.DIAMOND_ORE;
        }
        if (item == Items.NETHERITE_INGOT) {
            return Blocks.ANCIENT_DEBRIS;
        }
        return Blocks.COBBLESTONE;
    }
}
