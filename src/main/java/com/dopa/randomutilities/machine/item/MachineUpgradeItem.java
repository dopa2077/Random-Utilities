package com.dopa.randomutilities.machine.item;

import com.dopa.randomutilities.fishnet.FishnetBlock;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.generator.ResourceGeneratorBlock;
import com.dopa.randomutilities.machine.solarfurnace.SolarFurnaceBlock;

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
        /** Fishnet-only; each upgrade adds config % chance to force treasure in open water. */
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

    /**
     * Allow shift-right-click to reach the generator block (vanilla otherwise suppresses
     * block use while sneaking with an item in hand).
     */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        var block = level.getBlockState(pos).getBlock();
        return block instanceof ResourceGeneratorBlock
                || block instanceof SolarFurnaceBlock
                || block instanceof FishnetBlock;
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
        if (kind.showsPercent()) {
            line.append(Component.literal(kind.percent() + "%").withStyle(ChatFormatting.GREEN));
        }
        tooltip.accept(line);
    }
}
