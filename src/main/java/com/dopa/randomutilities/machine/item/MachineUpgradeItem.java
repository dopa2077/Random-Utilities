package com.dopa.randomutilities.machine.item;

import com.dopa.randomutilities.machine.config.UpgradeConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Productivity / overclock upgrade with a config-driven tooltip. */
public class MachineUpgradeItem extends Item {
    public enum Kind {
        PRODUCTIVITY(
                "item.dopasrandomutilities.productivity_upgrade.tooltip",
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
