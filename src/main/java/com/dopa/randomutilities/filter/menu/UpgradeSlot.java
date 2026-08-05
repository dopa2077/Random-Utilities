package com.dopa.randomutilities.filter.menu;

import com.dopa.randomutilities.util.PanelLayout;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Placeholder upgrade slot. Rejects all items until real upgrades exist.
 */
public class UpgradeSlot extends Slot {
    public static final int COUNT = 6;
    public static final int COLS = 3;
    public static final int ROWS = 2;

    /** Relative to GUI origin when the upgrade panel is fully open (right side). */
    public static final int TITLE_GAP = 18;

    private boolean active;

    public UpgradeSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public static int panelWidth() {
        return Math.max(PanelLayout.CONTENT_PAD * 2 + COLS * 18, 100);
    }

    public static int gridWidth() {
        return COLS * 18;
    }

    /** Body left edge relative to the inventory GUI (matches AttachedPanel bodyXOpen nudge). */
    public static int bodyOriginX() {
        return PanelLayout.GUI_WIDTH - 1;
    }

    public static int gridOriginX() {
        return bodyOriginX() + (panelWidth() - gridWidth()) / 2;
    }

    public static int gridOriginY() {
        return PanelLayout.BELOW_TAB_Y + PanelLayout.CONTENT_PAD + TITLE_GAP;
    }

    public static int slotX(int index) {
        return gridOriginX() + (index % COLS) * 18;
    }

    public static int slotY(int index) {
        return gridOriginY() + (index / COLS) * 18;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }
}
