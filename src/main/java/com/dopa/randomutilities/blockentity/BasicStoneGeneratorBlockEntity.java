package com.dopa.randomutilities.blockentity;

import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.GeneratorRecipeMatcher;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BasicStoneGeneratorBlockEntity extends BlockEntity {
    private int tickProgress;
    private boolean hasActiveMatch;
    private String activeRecipeId = "";
    private BlockPos fluid1Pos = BlockPos.ZERO;
    private BlockPos fluid2Pos = BlockPos.ZERO;

    public BasicStoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASIC_STONE_GENERATOR.get(), pos, state);
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

        if (match == null) {
            resetProgress();
            return;
        }

        if (!match.recipe().id().equals(activeRecipeId)
                || !match.fluid1Pos().equals(fluid1Pos)
                || !match.fluid2Pos().equals(fluid2Pos)) {
            activeRecipeId = match.recipe().id();
            fluid1Pos = match.fluid1Pos().immutable();
            fluid2Pos = match.fluid2Pos().immutable();
            hasActiveMatch = true;
            tickProgress = 0;
        }

        tickProgress++;

        if (tickProgress < match.recipe().ticks()) {
            setChanged();
            return;
        }

        BlockPos outputPos = worldPosition.above();
        BlockState outputSpace = level.getBlockState(outputPos);
        if (!outputSpace.canBeReplaced()) {
            tickProgress = match.recipe().ticks();
            setChanged();
            return;
        }

        BlockState resultState = match.recipe().result().defaultBlockState();
        level.setBlock(outputPos, resultState, Block.UPDATE_ALL);

        if (match.recipe().consume1()) {
            level.setBlock(fluid1Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (match.recipe().consume2()) {
            level.setBlock(fluid2Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        resetProgress();
        setChanged();
    }

    private void resetProgress() {
        tickProgress = 0;
        hasActiveMatch = false;
        activeRecipeId = "";
        fluid1Pos = BlockPos.ZERO;
        fluid2Pos = BlockPos.ZERO;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tickProgress = input.getIntOr("TickProgress", 0);
        hasActiveMatch = input.getBooleanOr("HasActiveMatch", false);
        activeRecipeId = input.getStringOr("ActiveRecipeId", "");
        if (hasActiveMatch) {
            fluid1Pos = new BlockPos(
                    input.getIntOr("Fluid1X", 0),
                    input.getIntOr("Fluid1Y", 0),
                    input.getIntOr("Fluid1Z", 0)
            );
            fluid2Pos = new BlockPos(
                    input.getIntOr("Fluid2X", 0),
                    input.getIntOr("Fluid2Y", 0),
                    input.getIntOr("Fluid2Z", 0)
            );
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
        if (hasActiveMatch) {
            output.putInt("Fluid1X", fluid1Pos.getX());
            output.putInt("Fluid1Y", fluid1Pos.getY());
            output.putInt("Fluid1Z", fluid1Pos.getZ());
            output.putInt("Fluid2X", fluid2Pos.getX());
            output.putInt("Fluid2Y", fluid2Pos.getY());
            output.putInt("Fluid2Z", fluid2Pos.getZ());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
