package com.dopa.randomutilities.block.cardboardbox;

import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class CardboardBoxBlockEntity extends BlockEntity {
    private @Nullable CardboardBoxContents contents;

    public CardboardBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARDBOARD_BOX.get(), pos, state);
    }

    public boolean hasContents() {
        return contents != null && contents.hasData();
    }

    public @Nullable CardboardBoxContents contents() {
        return contents;
    }

    public void setContents(@Nullable CardboardBoxContents contents) {
        this.contents = contents;
        syncFilledState();
        setChanged();
    }

    private void syncFilledState() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        BlockState current = this.getBlockState();
        boolean filled = hasContents();
        if (current.getValue(CardboardBoxBlock.FILLED) != filled) {
            this.level.setBlock(this.worldPosition, current.setValue(CardboardBoxBlock.FILLED, filled), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.contents = input.read("contents", CardboardBoxContents.CODEC).orElse(null);
        syncFilledState();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (contents != null && contents.hasData()) {
            output.store("contents", CardboardBoxContents.CODEC, contents);
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        syncFilledState();
    }
}
