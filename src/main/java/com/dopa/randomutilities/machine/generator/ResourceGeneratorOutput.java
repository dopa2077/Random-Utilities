package com.dopa.randomutilities.machine.generator;

import com.dopa.randomutilities.machine.generator.config.GeneratorOutputMode;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipe;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
            return insertFluid(level, pos, recipe.resultFluid(), amount, true) > 0;
        }
        if (outputOverride != null) {
            return outputBlocks(level, pos, outputOverride, amount, recipe.outputMode(), true) > 0;
        }
        if (recipe.isRandomResult()) {
            if (poolFor(type).isEmpty()) {
                return false;
            }
            return switch (recipe.outputMode()) {
                case INSERT -> getItemOutputHandler(level, pos) != null;
                case DROP -> canAcceptItemDrop(level, pos);
                case PLACE -> canAcceptPlace(level, pos);
            };
        }
        return outputBlocks(level, pos, recipe.result(), amount, recipe.outputMode(), true) > 0;
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
            case INSERT -> insertItems(level, pos, result, amount, simulate);
            case DROP -> simulate || dropItems(level, pos, result, amount) ? amount : 0;
            case PLACE -> simulate ? (canAcceptPlace(level, pos) ? 1 : 0) : (placeBlock(level, pos, result) ? 1 : 0);
        };
    }

    static int insertItems(ServerLevel level, BlockPos pos, Block result, int amount, boolean simulate) {
        ResourceHandler<ItemResource> handler = getItemOutputHandler(level, pos);
        if (handler == null || amount <= 0) {
            return 0;
        }
        ItemResource resource = ItemResource.of(result.asItem());
        if (resource.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.open(null)) {
            int inserted = handler.insert(resource, amount, tx);
            if (inserted > 0 && !simulate) {
                tx.commit();
            }
            return Math.max(0, inserted);
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
            if (insertedMb > 0 && !simulate) {
                tx.commit();
            }
            return Math.max(0, insertedMb);
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
        return level.getBlockState(above).canBeReplaced()
                && level.getEntitiesOfClass(ItemEntity.class, new AABB(above).inflate(1.5)).size() < MAX_NEARBY_ITEMS;
    }

    private static boolean placeBlock(ServerLevel level, BlockPos pos, Block result) {
        BlockPos above = pos.above();
        return canAcceptPlace(level, pos)
                && level.setBlock(above, result.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean dropItems(ServerLevel level, BlockPos pos, Block result, int amount) {
        if (!canAcceptItemDrop(level, pos)) {
            return false;
        }
        ItemStack probe = new ItemStack(result.asItem(), 1);
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
            ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(result.asItem(), batch));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            remaining -= batch;
        }
        return true;
    }
}
