package com.dopa.randomutilities.blockentity;

import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.GeneratorRecipeMatcher;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class BasicStoneGeneratorBlockEntity extends BlockEntity {
    private static final int EFFECT_INTERVAL_TICKS = 12;

    private int tickProgress;
    private boolean hasActiveMatch;
    private String activeRecipeId = "";
    private final BlockPos[] resourcePositions = new BlockPos[GeneratorRecipe.SIDE_COUNT];
    private final boolean[] hasResource = new boolean[GeneratorRecipe.SIDE_COUNT];

    public BasicStoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASIC_STONE_GENERATOR.get(), pos, state);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BasicStoneGeneratorBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }
        blockEntity.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        GeneratorRecipeMatcher.Match match = GeneratorRecipeMatcher
                .findBestMatch(level, worldPosition, GeneratorRecipeConfig.getRecipes())
                .orElse(null);

        if (match == null || !canAcceptResult(level, match.recipe())) {
            resetProgress();
            return;
        }

        if (!sameMatch(match)) {
            applyMatch(match);
            tickProgress = 0;
        }

        tickProgress++;
        spawnRunningEffects(level);

        if (tickProgress < match.recipe().ticks()) {
            setChanged();
            return;
        }

        if (!tryInsertResult(level, match.recipe())) {
            // Container filled up during this tick; idle until space is available again.
            resetProgress();
            setChanged();
            return;
        }

        consumeResources(level, match);
        resetProgress();
        setChanged();
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

    private boolean canAcceptResult(ServerLevel level, GeneratorRecipe recipe) {
        ResourceHandler<ItemResource> handler = getOutputHandler(level);
        if (handler == null) {
            return false;
        }

        ItemStack toInsert = new ItemStack(recipe.result().asItem(), recipe.amount());
        if (toInsert.isEmpty()) {
            return false;
        }

        return ItemUtil.insertItemReturnRemaining(handler, toInsert, true, null).isEmpty();
    }

    private boolean tryInsertResult(ServerLevel level, GeneratorRecipe recipe) {
        ResourceHandler<ItemResource> handler = getOutputHandler(level);
        if (handler == null) {
            return false;
        }

        ItemStack toInsert = new ItemStack(recipe.result().asItem(), recipe.amount());
        if (toInsert.isEmpty()) {
            return false;
        }

        return ItemUtil.insertItemReturnRemaining(handler, toInsert, false, null).isEmpty();
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

    private void spawnRunningEffects(ServerLevel level) {
        if (tickProgress % EFFECT_INTERVAL_TICKS != 0) {
            return;
        }

        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.05;
        double z = worldPosition.getZ() + 0.5;

        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.12, 0.05, 0.12, 0.005);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.08, 0.02, 0.08, 0.002);
        level.playSound(
                null,
                worldPosition,
                SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                SoundSource.BLOCKS,
                0.45F,
                0.75F + level.getRandom().nextFloat() * 0.15F
        );
    }

    private void resetProgress() {
        tickProgress = 0;
        hasActiveMatch = false;
        activeRecipeId = "";
        Arrays.fill(hasResource, false);
        Arrays.fill(resourcePositions, BlockPos.ZERO);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tickProgress = input.getIntOr("TickProgress", 0);
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
