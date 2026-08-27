package com.dopa.randomutilities.item.magnet;

import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.item.magnet.config.MagnetConfig;
import com.dopa.randomutilities.registry.ModDataComponents;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MagnetStorage {
    private static final CustomModelData ENABLED_MODEL_MARKER =
            new CustomModelData(List.of(), List.of(true), List.of(), List.of());

    private MagnetStorage() {}

    public static boolean isMagnet(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MagnetItem;
    }

    public static MagnetContents get(ItemStack stack) {
        if (!isMagnet(stack)) {
            return MagnetContents.defaults();
        }
        MagnetContents contents = stack.getOrDefault(ModDataComponents.MAGNET_CONTENTS.get(), MagnetContents.defaults());
        int max = maxRange(contents);
        if (contents.range() > max) {
            return contents.withRange(max);
        }
        return contents;
    }

    public static void set(ItemStack stack, MagnetContents contents) {
        if (!isMagnet(stack)) {
            return;
        }
        int max = maxRange(contents);
        if (contents.range() > max) {
            contents = contents.withRange(max);
        }
        stack.set(ModDataComponents.MAGNET_CONTENTS.get(), contents);
        if (contents.enabled()) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, ENABLED_MODEL_MARKER);
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    public static void toggle(ItemStack stack) {
        MagnetContents contents = get(stack);
        set(stack, contents.withEnabled(!contents.enabled()));
    }

    public static int countUpgrade(MagnetContents contents, Item item) {
        int total = 0;
        for (ItemStack stack : contents.upgrades()) {
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static int rangeCount(MagnetContents contents) {
        return countUpgrade(contents, ModItems.RANGE_UPGRADE.get());
    }

    public static int overclockCount(MagnetContents contents) {
        return countUpgrade(contents, ModItems.OVERCLOCK_UPGRADE.get());
    }

    public static int stackCount(MagnetContents contents) {
        return countUpgrade(contents, ModItems.STACK_UPGRADE.get());
    }

    public static int maxRange(ItemStack stack) {
        return maxRange(get(stack));
    }

    public static int maxRange(MagnetContents contents) {
        return MagnetConfig.maxRange() + UpgradeConfig.extraRange(rangeCount(contents));
    }

    public static int clampRange(MagnetContents contents, int value) {
        return Math.max(0, Math.min(maxRange(contents), value));
    }

    public static int tickInterval(MagnetContents contents) {
        return UpgradeConfig.effectiveTicks(MagnetConfig.baseTicks(), overclockCount(contents));
    }

    public static int pickupBudget(MagnetContents contents) {
        int stackCount = stackCount(contents);
        int total = UpgradeConfig.stackCollectorTotal(MagnetConfig.basePickupBatch(), stackCount);
        return Math.min(MagnetConfig.maxPickupBatch(), total);
    }

    /** XP orbs affected per magnet pulse (separate from item pickup budget). */
    public static int xpOrbBudget(MagnetContents contents) {
        return MagnetConfig.baseEntities();
    }

    @Nullable
    public static ItemStack findActive(Player player) {
        ItemStack main = player.getMainHandItem();
        if (isEnabled(main)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (isEnabled(off)) {
            return off;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == main || stack == off) {
                continue;
            }
            if (isEnabled(stack)) {
                return stack;
            }
        }
        return null;
    }

    private static boolean isEnabled(ItemStack stack) {
        return isMagnet(stack) && get(stack).enabled();
    }
}
