package com.dopa.randomutilities.item;

import com.dopa.randomutilities.config.DevNullConfig;
import com.dopa.randomutilities.filtersystem.FilterItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Creative-only FilterItem that exposes every AttachedPanel for UI testing. */
public class UiTestItem extends FilterItem {
    public UiTestItem(Properties properties) {
        super(properties, DevNullConfig.uiTestProfile());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.dopasrandomutilities.ui_test.tooltip")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
