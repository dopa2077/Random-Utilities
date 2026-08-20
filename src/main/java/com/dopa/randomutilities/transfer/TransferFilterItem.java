package com.dopa.randomutilities.transfer;

import java.util.function.Consumer;

import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.transfer.menu.TransferFilterMenu;

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

public class TransferFilterItem extends Item {
    public TransferFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        openGui(player, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        openGui(player, context.getHand());
        return InteractionResult.SUCCESS;
    }

    public static void openGui(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack host = player.getItemInHand(hand);
        if (!(host.getItem() instanceof TransferFilterItem)) {
            return;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new TransferFilterMenu(id, inv, hand),
                        Component.translatable("container.dopasrandomutilities.filter")
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
        tooltip.accept(Component.translatable("item.dopasrandomutilities.filter.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());
        TransferFilterContents.get(stack).appendHoverText(tooltip);
    }
}
