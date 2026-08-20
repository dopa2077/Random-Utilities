package com.dopa.randomutilities.machine.item;

import com.dopa.randomutilities.blockbreaker.AdvancedBlockBreakerBlock;
import com.dopa.randomutilities.blockplacer.AdvancedBlockPlacerBlock;
import com.dopa.randomutilities.fishnet.FishnetBlock;
import com.dopa.randomutilities.itemcollector.ItemCollectorBlock;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.generator.ResourceGeneratorBlock;
import com.dopa.randomutilities.solarfurnace.SolarFurnaceBlock;
import com.dopa.randomutilities.transfer.TransferNodeBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.LevelReader;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Machine / fishnet upgrade with a config-driven tooltip. */
public class MachineUpgradeItem extends Item {
    public enum Kind {
        PRODUCTIVITY(
                "item.dopasrandomutilities.productivity_upgrade.tooltip",
                UpgradeConfig::productivityBonusPercent,
                true
        ),
        OVERCLOCK(
                "item.dopasrandomutilities.overclock_upgrade.tooltip",
                UpgradeConfig::overclockSpeedPercent,
                true
        ),
        /** Fishnet-only; unlocks vanilla treasure in open water and adds a capped chance to force it. */
        FORTUNE_MESH(
                "item.dopasrandomutilities.fortune_mesh_upgrade.tooltip",
                UpgradeConfig::fortuneMeshTreasurePercent,
                true
        ),
        /** Fishnet-only; rolls custom treasure_loot.json instead of fishing loot. */
        TREASURE_MESH(
                "item.dopasrandomutilities.treasure_mesh_upgrade.tooltip",
                () -> 0,
                false
        ),
        ENERGY(
                "item.dopasrandomutilities.energy_upgrade.tooltip",
                UpgradeConfig::energyBonusPercent,
                true
        ),
        EFFICIENCY(
                "item.dopasrandomutilities.efficiency_upgrade.tooltip",
                UpgradeConfig::efficiencyBonusPercent,
                true
        ),
        RANGE(
                "item.dopasrandomutilities.range_upgrade.tooltip",
                UpgradeConfig::rangeBonus,
                false
        ),
        STACK(
                "item.dopasrandomutilities.stack_upgrade.tooltip",
                () -> 0,
                false
        ),
        FLUID_CAPACITY(
                "item.dopasrandomutilities.fluid_capacity_upgrade.tooltip",
                UpgradeConfig::fluidCapacityBonusPercent,
                true
        );

        private final String tooltipKey;
        private final IntSupplier percent;
        private final boolean showsPercent;

        Kind(String tooltipKey, IntSupplier percent, boolean showsPercent) {
            this.tooltipKey = tooltipKey;
            this.percent = percent;
            this.showsPercent = showsPercent;
        }

        public String tooltipKey() {
            return tooltipKey;
        }

        public int percent() {
            return percent.getAsInt();
        }

        public boolean showsPercent() {
            return showsPercent;
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

    /** Gray description used both in the inventory hover and in installed-slot tooltips. */
    public Component descriptionLine() {
        if (kind == Kind.RANGE) {
            return Component.translatable(
                    kind.tooltipKey(),
                    Component.literal(Integer.toString(kind.percent())).withStyle(ChatFormatting.GREEN)
            ).withStyle(ChatFormatting.GRAY);
        }
        MutableComponent line = Component.translatable(kind.tooltipKey()).withStyle(ChatFormatting.GRAY);
        if (kind.showsPercent()) {
            line.append(Component.literal(kind.percent() + "%").withStyle(ChatFormatting.GREEN));
        }
        return line;
    }

    /**
     * Allow shift-right-click to reach the generator block (vanilla otherwise suppresses
     * block use while sneaking with an item in hand).
     */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        var block = level.getBlockState(pos).getBlock();
        return block instanceof ResourceGeneratorBlock
                || block instanceof SolarFurnaceBlock
                || block instanceof FishnetBlock
                || block instanceof AdvancedBlockBreakerBlock
                || block instanceof AdvancedBlockPlacerBlock
                || block instanceof ItemCollectorBlock
                || block instanceof TransferNodeBlock;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(descriptionLine());
        if (kind == Kind.ENERGY) {
            tooltip.accept(Component.translatable("item.dopasrandomutilities.energy_upgrade.tooltip_node")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
