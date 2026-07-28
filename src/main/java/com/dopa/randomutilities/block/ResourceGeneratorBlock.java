package com.dopa.randomutilities.block;

import com.dopa.randomutilities.blockentity.ResourceGeneratorBlockEntity;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ResourceGeneratorBlock extends BaseEntityBlock {
    private final GeneratorType generatorType;
    private final MapCodec<ResourceGeneratorBlock> codec;

    public ResourceGeneratorBlock(Properties properties, GeneratorType generatorType) {
        super(properties);
        this.generatorType = generatorType;
        this.codec = simpleCodec(props -> new ResourceGeneratorBlock(props, generatorType));
    }

    public GeneratorType generatorType() {
        return generatorType;
    }

    @Override
    protected MapCodec<? extends ResourceGeneratorBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide()
                ? null
                : createTickerHelper(
                        type,
                        ModBlockEntities.RESOURCE_GENERATOR.get(),
                        ResourceGeneratorBlockEntity::serverTick
                );
    }
}
