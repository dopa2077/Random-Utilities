package com.dopa.randomutilities.cardboardbox;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Consumer;

public class CardboardBoxItem extends BlockItem {
    public CardboardBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        if (!CardboardBoxContents.has(stack)) {
            Optional<Component> failure = CardboardBoxLogic.tryWrapFromItem(context);
            if (failure.isPresent()) {
                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(failure.get(), true);
                }
                return InteractionResult.FAIL;
            }
            return InteractionResult.CONSUME;
        }
        return super.useOn(context);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            BlockPos pos,
            Level level,
            @org.jspecify.annotations.Nullable Player player,
            ItemStack stack,
            BlockState state
    ) {
        if (level.getBlockEntity(pos) instanceof CardboardBoxBlockEntity box) {
            CardboardBoxContents contents = CardboardBoxContents.get(stack);
            box.setContents(contents);
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.dopasrandomutilities.cardboard_box.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());
        CardboardBoxTooltip.append(stack, context.registries(), tooltip);
    }
}
