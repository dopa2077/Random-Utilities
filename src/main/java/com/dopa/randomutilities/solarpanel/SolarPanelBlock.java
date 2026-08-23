package com.dopa.randomutilities.solarpanel;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Flat solar panel slab; ownership BE only (no UI / no ticker). */
public class SolarPanelBlock extends Block implements EntityBlock {
    public static final MapCodec<SolarPanelBlock> CODEC =
            simpleCodec(properties -> new SolarPanelBlock(properties, SolarPanelTier.TIER1));
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    private final SolarPanelTier tier;

    public SolarPanelBlock(Properties properties, SolarPanelTier tier) {
        super(properties);
        this.tier = tier;
    }

    public SolarPanelTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends SolarPanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }
}
