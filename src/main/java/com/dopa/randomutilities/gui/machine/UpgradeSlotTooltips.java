package com.dopa.randomutilities.gui.machine;

import com.dopa.randomutilities.machine.UpgradeInventory;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.item.MachineUpgradeItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Installed-upgrade hover lines (total + available) shared by machine screens. */
public final class UpgradeSlotTooltips {
    private UpgradeSlotTooltips() {}

    public static boolean applyHover(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            @Nullable Slot hovered,
            boolean upgradeSlot,
            UpgradeInventory upgrades
    ) {
        return applyHover(graphics, font, mouseX, mouseY, hovered, upgradeSlot, upgrades, false, List.of(), null, false);
    }

    public static boolean applyHover(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            @Nullable Slot hovered,
            boolean upgradeSlot,
            UpgradeInventory upgrades,
            boolean energyVoidConfirm
    ) {
        return applyHover(
                graphics, font, mouseX, mouseY, hovered, upgradeSlot, upgrades, energyVoidConfirm, List.of(), null, false);
    }

    public static boolean applyHover(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            @Nullable Slot hovered,
            boolean upgradeSlot,
            UpgradeInventory upgrades,
            List<Component> extras,
            @Nullable Component totalOverride
    ) {
        return applyHover(
                graphics, font, mouseX, mouseY, hovered, upgradeSlot, upgrades, false, extras, totalOverride, false);
    }

    public static boolean applyHover(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            @Nullable Slot hovered,
            boolean upgradeSlot,
            UpgradeInventory upgrades,
            List<Component> extras,
            @Nullable Component totalOverride,
            boolean strikethroughStats
    ) {
        return applyHover(
                graphics,
                font,
                mouseX,
                mouseY,
                hovered,
                upgradeSlot,
                upgrades,
                false,
                extras,
                totalOverride,
                strikethroughStats
        );
    }

    private static boolean applyHover(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            @Nullable Slot hovered,
            boolean upgradeSlot,
            UpgradeInventory upgrades,
            boolean energyVoidConfirm,
            List<Component> extras,
            @Nullable Component totalOverride,
            boolean strikethroughStats
    ) {
        if (hovered == null || !upgradeSlot || !hovered.hasItem()) {
            return false;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (energyVoidConfirm) {
            lines.add(Component.translatable("gui.dopasrandomutilities.upgrade.energy.void_warning")
                    .withStyle(ChatFormatting.RED)
                    .getVisualOrderText());
            lines.add(Component.empty().getVisualOrderText());
            lines.add(Component.translatable("gui.dopasrandomutilities.remove_slot.tooltip_void_confirm")
                    .withStyle(ChatFormatting.DARK_RED)
                    .getVisualOrderText());
        } else {
            for (Component line : installed(hovered.getItem(), upgrades, extras, totalOverride, strikethroughStats)) {
                lines.add(line.getVisualOrderText());
            }
        }
        graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        return true;
    }

    public static List<Component> installed(ItemStack stack, UpgradeInventory upgrades) {
        return installed(stack, upgrades, List.of(), null, false);
    }

    public static List<Component> installed(
            ItemStack stack,
            UpgradeInventory upgrades,
            List<Component> extras,
            @Nullable Component totalOverride,
            boolean strikethroughStats
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        Item item = stack.getItem();
        int used = upgrades.countOf(item);
        int max = upgrades.maxFor(item);
        if (item instanceof MachineUpgradeItem upgrade) {
            Component description = upgrade.descriptionLine();
            if (strikethroughStats) {
                description = Component.literal(description.getString())
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
            }
            lines.add(description);
        }
        if (extras != null) {
            lines.addAll(extras);
        }
        Component total = totalOverride;
        if (total == null && item instanceof MachineUpgradeItem upgrade) {
            total = totalLine(upgrade.kind(), used, strikethroughStats);
        } else if (total != null && strikethroughStats) {
            total = Component.literal(total.getString())
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
        }
        if (total != null) {
            lines.add(Component.empty());
            lines.add(total);
        }
        ChatFormatting color = used >= max && max > 0 ? ChatFormatting.RED : ChatFormatting.GREEN;
        lines.add(Component.translatable(
                "gui.dopasrandomutilities.upgrade.available",
                Integer.toString(used),
                Integer.toString(max)
        ).withStyle(color));
        return lines;
    }

    @Nullable
    private static Component totalLine(MachineUpgradeItem.Kind kind, int used, boolean strikethroughStats) {
        if (kind == MachineUpgradeItem.Kind.TREASURE_MESH) {
            return null;
        }
        ChatFormatting labelColor = strikethroughStats ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY;
        ChatFormatting valueColor = strikethroughStats ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN;
        MutableComponent line = Component.translatable(totalKey(kind)).withStyle(labelColor);
        String value = totalValue(kind, used);
        if (value != null) {
            MutableComponent valueLine = Component.literal(value).withStyle(valueColor);
            if (strikethroughStats) {
                line.withStyle(ChatFormatting.STRIKETHROUGH);
                valueLine.withStyle(ChatFormatting.STRIKETHROUGH);
            }
            line.append(valueLine);
        } else if (strikethroughStats) {
            line.withStyle(ChatFormatting.STRIKETHROUGH);
        }
        return line;
    }

    private static String totalKey(MachineUpgradeItem.Kind kind) {
        return switch (kind) {
            case RANGE -> "gui.dopasrandomutilities.upgrade.total_range";
            case EFFICIENCY -> "gui.dopasrandomutilities.upgrade.total_efficiency";
            case ENERGY -> "gui.dopasrandomutilities.upgrade.total_increase";
            case STACK -> "gui.dopasrandomutilities.upgrade.total_stack";
            default -> "gui.dopasrandomutilities.upgrade.total_boost";
        };
    }

    @Nullable
    private static String totalValue(MachineUpgradeItem.Kind kind, int used) {
        return switch (kind) {
            case RANGE -> "+" + (used * UpgradeConfig.rangeBonus());
            case FORTUNE_MESH -> UpgradeConfig.fortuneMeshChancePercent(used) + "%";
            case ENERGY -> (used * 100) + "%";
            case STACK, TREASURE_MESH -> null;
            default -> (used * kind.percent()) + "%";
        };
    }
}
