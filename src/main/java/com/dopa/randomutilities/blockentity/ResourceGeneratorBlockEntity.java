package com.dopa.randomutilities.blockentity;

import com.dopa.randomutilities.block.ResourceGeneratorBlock;
import com.dopa.randomutilities.config.GeneratedBlockLists;
import com.dopa.randomutilities.config.GeneratorOutputMode;
import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.GeneratorRecipeMatcher;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class ResourceGeneratorBlockEntity extends BlockEntity {
    /** Independent of recipe tick speed so fast generators still play ambient effects. */
    private static final int EFFECT_INTERVAL_TICKS = 10;

    private int tickProgress;
    private int effectTimer;
    private boolean hasActiveMatch;
    private String activeRecipeId = "";
    private final BlockPos[] resourcePositions = new BlockPos[GeneratorRecipe.SIDE_COUNT];
    private final boolean[] hasResource = new boolean[GeneratorRecipe.SIDE_COUNT];

    public ResourceGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESOURCE_GENERATOR.get(), pos, state);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ResourceGeneratorBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }
        blockEntity.tick((ServerLevel) level);
    }

    private GeneratorType generatorType() {
        if (getBlockState().getBlock() instanceof ResourceGeneratorBlock block) {
            return block.generatorType();
        }
        return GeneratorType.BASIC_STONE;
    }

    private void tick(ServerLevel level) {
        GeneratorType type = generatorType();
        GeneratorRecipeMatcher.Match match = GeneratorRecipeMatcher
                .findBestMatch(level, worldPosition, GeneratorRecipeConfig.getRecipes(type))
                .orElse(null);

        if (match == null || !canStartOutput(level, type, match.recipe())) {
            resetProgress();
            return;
        }

        if (!sameMatch(match)) {
            applyMatch(match);
            tickProgress = 0;
        }

        tickProgress++;
        tickRunningEffects(level);

        if (tickProgress < match.recipe().ticks()) {
            setChanged();
            return;
        }

        Block result = resolveResult(level, type, match.recipe());
        if (result == null || !tryOutput(level, result, match.recipe().amount(), match.recipe().outputMode())) {
            resetProgress();
            setChanged();
            return;
        }

        consumeResources(level, match);
        // Keep ambient effects continuous across fast cycles; only clear recipe progress.
        tickProgress = 0;
        setChanged();
    }

    @Nullable
    private Block resolveResult(ServerLevel level, GeneratorType type, GeneratorRecipe recipe) {
        if (!recipe.isRandomResult()) {
            return recipe.result();
        }

        List<Block> pool = poolFor(type);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(level.getRandom().nextInt(pool.size()));
    }

    private List<Block> poolFor(GeneratorType type) {
        return switch (type.mode()) {
            case RANDOM_ORE -> GeneratedBlockLists.ores();
            case METAL_BLOCK -> GeneratedBlockLists.metalBlocks();
            case RECIPE -> List.of();
        };
    }

    private boolean canStartOutput(ServerLevel level, GeneratorType type, GeneratorRecipe recipe) {
        if (recipe.isRandomResult()) {
            if (poolFor(type).isEmpty()) {
                return false;
            }
            return switch (recipe.outputMode()) {
                case INSERT -> getOutputHandler(level) != null;
                case DROP -> canAcceptDrop(level, recipe.amount());
            };
        }
        return canOutput(level, recipe.result(), recipe.amount(), recipe.outputMode());
    }

    private boolean sameMatch(GeneratorRecipeMatcher.Match match) {
        if (!match.recipe().id().equals(activeRecipeId) || !hasActiveMatch) {
            return false;
        }
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            BlockPos matched = match.resourcePositions()[i];
            boolean present = matched != null;
            if (present != hasResource[i]) {
                return false;
            }
            if (present && !matched.equals(resourcePositions[i])) {
                return false;
            }
        }
        return true;
    }

    private void applyMatch(GeneratorRecipeMatcher.Match match) {
        activeRecipeId = match.recipe().id();
        hasActiveMatch = true;
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            BlockPos matched = match.resourcePositions()[i];
            hasResource[i] = matched != null;
            resourcePositions[i] = matched == null ? BlockPos.ZERO : matched.immutable();
        }
    }

    @Nullable
    private ResourceHandler<ItemResource> getOutputHandler(ServerLevel level) {
        return level.getCapability(Capabilities.Item.BLOCK, worldPosition.above(), Direction.DOWN);
    }

    private boolean canOutput(ServerLevel level, Block result, int amount, GeneratorOutputMode mode) {
        return switch (mode) {
            case INSERT -> tryInsertAmount(level, result, amount, true);
            case DROP -> canAcceptDrop(level, amount);
        };
    }

    private boolean tryOutput(ServerLevel level, Block result, int amount, GeneratorOutputMode mode) {
        return switch (mode) {
            case INSERT -> tryInsertAmount(level, result, amount, false);
            case DROP -> tryDropResult(level, result, amount);
        };
    }

    private boolean tryInsertAmount(ServerLevel level, Block result, int amount, boolean simulate) {
        ResourceHandler<ItemResource> handler = getOutputHandler(level);
        if (handler == null || amount <= 0) {
            return false;
        }

        ItemResource resource = ItemResource.of(result.asItem());
        if (resource.isEmpty()) {
            return false;
        }

        try (Transaction tx = Transaction.open(null)) {
            // Insert as many as fit (up to amount). Creative/absurd amounts should keep
            // filling remaining slots instead of waiting until the full amount fits.
            int inserted = handler.insert(resource, amount, tx);
            if (inserted <= 0) {
                return false;
            }
            if (!simulate) {
                tx.commit();
            }
            return true;
        }
    }

    private boolean canAcceptDrop(ServerLevel level, int amount) {
        if (amount > 1) {
            return true;
        }
        return level.getBlockState(worldPosition.above()).canBeReplaced();
    }

    private boolean tryDropResult(ServerLevel level, Block result, int amount) {
        BlockPos above = worldPosition.above();

        if (amount == 1) {
            BlockState existing = level.getBlockState(above);
            if (!existing.canBeReplaced()) {
                return false;
            }
            return level.setBlock(above, result.defaultBlockState(), Block.UPDATE_ALL);
        }

        ItemStack probe = new ItemStack(result.asItem(), 1);
        if (probe.isEmpty()) {
            return false;
        }

        double x = above.getX() + 0.5;
        double y = above.getY() + 0.15;
        double z = above.getZ() + 0.5;
        int maxStack = Math.max(1, probe.getMaxStackSize());
        int remaining = amount;

        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(result.asItem(), batch);
            ItemEntity entity = new ItemEntity(level, x, y, z, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            remaining -= batch;
        }
        return true;
    }

    private void consumeResources(ServerLevel level, GeneratorRecipeMatcher.Match match) {
        boolean[] consume = match.recipe().consume();
        BlockPos[] positions = match.resourcePositions();
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            if (!consume[i] || positions[i] == null) {
                continue;
            }
            level.setBlock(positions[i], Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void tickRunningEffects(ServerLevel level) {
        effectTimer++;
        if (effectTimer < EFFECT_INTERVAL_TICKS) {
            return;
        }
        effectTimer = 0;

        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.05;
        double z = worldPosition.getZ() + 0.5;

        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.12, 0.05, 0.12, 0.005);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.08, 0.02, 0.08, 0.0);
        level.playSound(
                null,
                worldPosition,
                SoundEvents.FURNACE_FIRE_CRACKLE,
                SoundSource.BLOCKS,
                0.18F,
                0.85F + level.getRandom().nextFloat() * 0.2F
        );
    }

    private void resetProgress() {
        tickProgress = 0;
        effectTimer = 0;
        hasActiveMatch = false;
        activeRecipeId = "";
        Arrays.fill(hasResource, false);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tickProgress = input.getIntOr("TickProgress", 0);
        effectTimer = input.getIntOr("EffectTimer", 0);
        hasActiveMatch = input.getBooleanOr("HasActiveMatch", false);
        activeRecipeId = input.getStringOr("ActiveRecipeId", "");
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            int index = i + 1;
            hasResource[i] = input.getBooleanOr("HasResource" + index, false);
            if (hasActiveMatch && hasResource[i]) {
                resourcePositions[i] = new BlockPos(
                        input.getIntOr("Resource" + index + "X", 0),
                        input.getIntOr("Resource" + index + "Y", 0),
                        input.getIntOr("Resource" + index + "Z", 0)
                );
            } else {
                resourcePositions[i] = BlockPos.ZERO;
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TickProgress", tickProgress);
        output.putInt("EffectTimer", effectTimer);
        output.putBoolean("HasActiveMatch", hasActiveMatch);
        if (!activeRecipeId.isEmpty()) {
            output.putString("ActiveRecipeId", activeRecipeId);
        }
        for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
            int index = i + 1;
            output.putBoolean("HasResource" + index, hasResource[i]);
            if (hasActiveMatch && hasResource[i]) {
                output.putInt("Resource" + index + "X", resourcePositions[i].getX());
                output.putInt("Resource" + index + "Y", resourcePositions[i].getY());
                output.putInt("Resource" + index + "Z", resourcePositions[i].getZ());
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
