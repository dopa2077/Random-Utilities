package com.dopa.randomutilities.item;

import com.dopa.randomutilities.config.UpgradeConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Capacity / overclock upgrade with a config-driven tooltip. */
public class MachineUpgradeItem extends Item {
    public enum Kind {
        CAPACITY(
                "item.dopasrandomutilities.capacity_upgrade.tooltip",
                UpgradeConfig::capacityBonusPercent
        ),
        OVERCLOCK(
                "item.dopasrandomutilities.overclock_upgrade.tooltip",
                UpgradeConfig::overclockSpeedPercent
        );

        private final String tooltipKey;
        private final IntSupplier percent;

        Kind(String tooltipKey, IntSupplier percent) {
            this.tooltipKey = tooltipKey;
            this.percent = percent;
        }

        public String tooltipKey() {
            return tooltipKey;
        }

        public int percent() {
            return percent.getAsInt();
        }
    }

    private final Kind kind;

    public MachineUpgradeItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        MutableComponent line = Component.translatable(kind.tooltipKey()).withStyle(ChatFormatting.GRAY);
        line.append(Component.literal(kind.percent() + "%").withStyle(ChatFormatting.GREEN));
        tooltip.accept(line);
    }
}
