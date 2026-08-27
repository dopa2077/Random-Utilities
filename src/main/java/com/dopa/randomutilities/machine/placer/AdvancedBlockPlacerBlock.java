package com.dopa.randomutilities.machine.placer;

import com.dopa.randomutilities.machine.placer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.core.machine.MachineBlocks;
import com.dopa.randomutilities.core.machine.OrientedEntityBlock;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AdvancedBlockPlacerBlock extends OrientedEntityBlock {
    public static final MapCodec<AdvancedBlockPlacerBlock> CODEC = simpleCodec(AdvancedBlockPlacerBlock::new);

    public AdvancedBlockPlacerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean usesTriggered() {
        return false;
    }

    @Override
    protected MapCodec<? extends AdvancedBlockPlacerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedBlockPlacerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof AdvancedBlockPlacerBlockEntity be)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return MachineBlocks.tryInsertUpgrade(player, stack, level, pos, be.upgrades());
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
        if (!(level.getBlockEntity(pos) instanceof AdvancedBlockPlacerBlockEntity be)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new AdvancedBlockPlacerMenu(id, inv, be),
                        be.getDisplayName()
                ),
                buf -> buf.writeBlockPos(pos)
        );
        return InteractionResult.CONSUME;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof AdvancedBlockPlacerBlockEntity be) {
            return be.getAnalogOutput();
        }
        return 0;
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
                        ModBlockEntities.ADVANCED_BLOCK_PLACER.get(),
                        AdvancedBlockPlacerBlockEntity::serverTick
                );
    }
}
