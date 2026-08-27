package com.dopa.randomutilities.core.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Shared serverbound ghost-filter payload handling: menu validity + identity-only stacks.
 */
public final class GhostFilterPayloads {
    private GhostFilterPayloads() {}

    public static boolean menuValid(Player player, AbstractContainerMenu menu) {
        return menu != null && menu == player.containerMenu && menu.stillValid(player);
    }

    /**
     * Ghost wells store item identity (and typed filter components), not arbitrary
     * {@link DataComponents#CUSTOM_DATA} blobs from spoofed/JEI packets.
     */
    public static ItemStack sanitizeGhost(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack ghost = stack.copyWithCount(1);
        ghost.remove(DataComponents.CUSTOM_DATA);
        return ghost;
    }
}
