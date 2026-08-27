package com.dopa.randomutilities.item.magnet;

import java.util.function.Consumer;

import com.dopa.randomutilities.item.magnet.menu.MagnetMenu;
import com.dopa.randomutilities.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class MagnetItem extends Item {
    public MagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack host = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            MagnetStorage.toggle(host);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        InteractionHand hand = context.getHand();
        if (player.isShiftKeyDown()) {
            openGui(player, hand);
            return InteractionResult.SUCCESS;
        }
        if (!context.getLevel().isClientSide()) {
            MagnetStorage.toggle(player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    public static void openGui(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack host = player.getItemInHand(hand);
        if (!MagnetStorage.isMagnet(host)) {
            return;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new MagnetMenu(id, inv, hand),
                        Component.translatable("container.dopasrandomutilities.item_magnet")
                ),
                buf -> buf.writeEnum(hand)
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.dopasrandomutilities.item_magnet.tooltip")
                .withStyle(ChatFormatting.GRAY));
        MagnetContents contents = stack.getOrDefault(
                ModDataComponents.MAGNET_CONTENTS.get(),
                MagnetContents.defaults()
        );
        tooltip.accept(Component.translatable(contents.enabled()
                        ? "item.dopasrandomutilities.item_magnet.enabled"
                        : "item.dopasrandomutilities.item_magnet.disabled")
                .withStyle(contents.enabled() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
    }
}
