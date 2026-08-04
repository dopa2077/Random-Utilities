package com.dopa.randomutilities.block;

import com.dopa.randomutilities.blockentity.UiTestBlockEntity;
import com.dopa.randomutilities.config.DevNullConfig;
import com.dopa.randomutilities.filtersystem.FilterContents;
import com.dopa.randomutilities.filtersystem.FilterStorage;
import com.dopa.randomutilities.filtersystem.menu.FilterMenu;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Placeable missing-texture host that opens the full FilterScreen for UI testing. */
public class UiTestBlock extends BaseEntityBlock {
    private static final MapCodec<UiTestBlock> CODEC = simpleCodec(UiTestBlock::new);

    public UiTestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends UiTestBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UiTestBlockEntity(pos, state);
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
        if (!(level.getBlockEntity(pos) instanceof UiTestBlockEntity be)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack host = be.hostStack();
        FilterContents contents = FilterStorage.get(host);
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new FilterMenu(id, inv, pos, contents),
                        DevNullConfig.uiTestProfile().containerTitle()
                ),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeBlockPos(pos);
                    FilterContents.STREAM_CODEC.encode(buf, contents);
                    buf.writeBoolean(false);
                }
        );
        return InteractionResult.CONSUME;
    }
}
