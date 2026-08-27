package com.dopa.randomutilities.item.lasso;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public final class LassoItem extends Item {
    private static final int VALUE_GREEN = 0x55FF55;

    private final LassoTier tier;

    public LassoItem(Properties properties, LassoTier tier) {
        super(properties);
        this.tier = tier;
    }

    private LassoTier tier() {
        return tier;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack handStack = player.getItemInHand(hand);
        if (LassoCapture.has(handStack)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return finish(serverPlayer, hand, handStack, LassoLogic.tryCapture(tier(), handStack, serverPlayer, target, hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack handStack = player.getItemInHand(context.getHand());
        if (tier() == LassoTier.CURSED
                && LassoCapture.has(handStack)
                && level.getBlockState(context.getClickedPos()).is(Blocks.SPAWNER)) {
            Optional<Component> failure = LassoSpawnerRewrite.tryRewrite(
                    (ServerLevel) level,
                    serverPlayer,
                    context.getClickedPos(),
                    level.getBlockState(context.getClickedPos()),
                    handStack
            );
            return finish(
                    serverPlayer,
                    context.getHand(),
                    handStack,
                    failure,
                    InteractionResult.CONSUME
            );
        }

        if (!LassoCapture.has(handStack)) {
            return InteractionResult.PASS;
        }

        return finish(
                serverPlayer,
                context.getHand(),
                handStack,
                LassoLogic.tryDeploy(
                        tier(),
                        handStack,
                        serverPlayer,
                        context.getClickedPos().relative(context.getClickedFace()),
                        context.getHand()
                )
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return LassoCapture.has(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!LassoCapture.has(stack) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return finish(
                serverPlayer,
                hand,
                stack,
                LassoLogic.tryDeploy(
                        tier(),
                        stack,
                        serverPlayer,
                        LassoLogic.deployPosFromUse(player),
                        hand
                )
        );
    }

    private static InteractionResult finish(
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack,
            Optional<Component> failure
    ) {
        return finish(player, hand, stack, failure, InteractionResult.SUCCESS);
    }

    private static InteractionResult finish(
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack,
            Optional<Component> failure,
            InteractionResult onSuccess
    ) {
        if (failure.isPresent()) {
            player.sendSystemMessage(failure.get(), true);
            return InteractionResult.FAIL;
        }
        player.setItemInHand(hand, stack);
        return onSuccess;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable(descriptionKey()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());
        LassoCapture capture = LassoCapture.get(stack);
        if (capture == null) {
            tooltip.accept(Component.translatable("item.dopasrandomutilities.lasso.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(labelValue(
                    "item.dopasrandomutilities.lasso.mob",
                    capture.mobName().copy().withColor(VALUE_GREEN)
            ));
            tooltip.accept(labelValue(
                    "item.dopasrandomutilities.lasso.health",
                    Component.literal(formatHearts(capture.health()) + " / " + formatHearts(capture.maxHealth()))
                            .withColor(VALUE_GREEN)
            ));
        }
    }

    private static Component labelValue(String labelKey, Component value) {
        return Component.translatable(labelKey, value)
                .withStyle(style -> style.withColor(ChatFormatting.AQUA));
    }

    private String descriptionKey() {
        return switch (tier()) {
            case BASIC -> "item.dopasrandomutilities.lasso.tooltip";
            case GOLDEN -> "item.dopasrandomutilities.golden_lasso.tooltip";
            case CURSED -> "item.dopasrandomutilities.cursed_lasso.tooltip";
        };
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false;
    }

    /** Formats entity health points as hearts (1 heart = 2 HP). */
    private static String formatHearts(float healthPoints) {
        return String.format(Locale.ROOT, "%.1f", healthPoints / 2.0F);
    }
}
