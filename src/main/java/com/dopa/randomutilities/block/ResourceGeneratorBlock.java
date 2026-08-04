package com.dopa.randomutilities.block;

import com.dopa.randomutilities.blockentity.ResourceGeneratorBlockEntity;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.machine.generator.menu.ResourceGeneratorMenu;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ResourceGeneratorBlock extends BaseEntityBlock {
    /** Glass window faces this direction (model glass is on the south face). */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private final GeneratorType generatorType;
    private final MapCodec<ResourceGeneratorBlock> codec;

    public ResourceGeneratorBlock(Properties properties, GeneratorType generatorType) {
        super(properties);
        this.generatorType = generatorType;
        this.codec = simpleCodec(props -> new ResourceGeneratorBlock(props, generatorType));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ResourceGeneratorBlockEntity be)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new ResourceGeneratorMenu(id, inv, be),
                        state.getBlock().getName()
                ),
                buf -> buf.writeBlockPos(pos)
        );
        return InteractionResult.CONSUME;
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
