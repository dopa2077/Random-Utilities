package com.dopa.randomutilities.item.wrench;

import com.dopa.randomutilities.core.machine.WrenchRotations;
import com.dopa.randomutilities.machine.solar.panel.SolarPanelControllerBlock;
import com.dopa.randomutilities.machine.solar.panel.SolarPanelControllerBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Consumer;

public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (player.isShiftKeyDown() && state.getBlock() instanceof SolarPanelControllerBlock) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof SolarPanelControllerBlockEntity controller)
                    || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (controller.isFormed()) {
                controller.unform(null);
                serverPlayer.sendSystemMessage(
                        Component.translatable("item.dopasrandomutilities.wrench.form.unformed"),
                        true
                );
                return InteractionResult.SUCCESS;
            }
            Optional<Component> failure = controller.tryRecast();
            if (failure.isPresent()) {
                serverPlayer.sendSystemMessage(failure.get(), true);
                return InteractionResult.FAIL;
            }
            serverPlayer.sendSystemMessage(
                    Component.translatable(
                            "item.dopasrandomutilities.wrench.form.success",
                            controller.linkedPanels()
                    ),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        if (!WrenchRotations.isRotatable(state)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean changed = WrenchRotations.tryRotate(level, pos, state, context.getClickedFace());
        if (changed) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.dopasrandomutilities.wrench.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
