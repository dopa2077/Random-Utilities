package com.dopa.randomutilities.block.cardboardbox;

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
        ItemStack stack = context.getItemInHand();
        if (CardboardBoxContents.has(stack)) {
            return super.useOn(context);
        }

        return switch (getEmptyBoxAction(context)) {
            case PASS -> InteractionResult.PASS;
            case PLACE -> super.useOn(context);
            case WRAP -> {
                Level level = context.getLevel();
                if (level.isClientSide()) {
                    yield InteractionResult.SUCCESS;
                }
                Optional<Component> failure = CardboardBoxLogic.tryWrapFromItem(context);
                if (failure.isPresent()) {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(failure.get(), true);
                    }
                    yield InteractionResult.FAIL;
                }
                yield InteractionResult.CONSUME;
            }
        };
    }

    private static EmptyBoxUseAction getEmptyBoxAction(UseOnContext context) {
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isShiftKeyDown();
        boolean hasBlockEntity = context.getLevel().getBlockEntity(context.getClickedPos()) != null;
        if (hasBlockEntity) {
            return sneaking ? EmptyBoxUseAction.WRAP : EmptyBoxUseAction.PASS;
        }
        return sneaking ? EmptyBoxUseAction.PLACE : EmptyBoxUseAction.WRAP;
    }

    private enum EmptyBoxUseAction {
        PASS,
        WRAP,
        PLACE
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
