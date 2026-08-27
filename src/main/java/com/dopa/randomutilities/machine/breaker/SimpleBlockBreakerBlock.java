package com.dopa.randomutilities.machine.breaker;

import com.dopa.randomutilities.core.machine.OrientedEntityBlock;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SimpleBlockBreakerBlock extends OrientedEntityBlock {
    public static final MapCodec<SimpleBlockBreakerBlock> CODEC = simpleCodec(SimpleBlockBreakerBlock::new);

    public SimpleBlockBreakerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleBlockBreakerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleBlockBreakerBlockEntity(pos, state);
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
                        ModBlockEntities.SIMPLE_BLOCK_BREAKER.get(),
                        SimpleBlockBreakerBlockEntity::serverTick
                );
    }
}
